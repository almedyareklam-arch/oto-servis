from pathlib import Path

main = Path("app/src/main/java/com/ucuzcu/app/MainActivity.java")
s = main.read_text(encoding="utf-8")

s = s.replace("Ucuzcu/0.7", "Ucuzcu/0.8")

old_field = '    private String currentQuery = "";\n'
new_field = '    private String currentQuery = "";\n    private String searchQuery = "";\n'
if old_field not in s:
    raise SystemExit("V8 searchQuery field injection point not found")
s = s.replace(old_field, new_field, 1)

old_start = '''        currentQuery = q;\n        collected.clear();'''
new_start = '''        currentQuery = q;\n        searchQuery = correctKnownBrands(q);\n        collected.clear();'''
if old_start not in s:
    raise SystemExit("V8 corrected search query injection point not found")
s = s.replace(old_start, new_start, 1)

old_url = '''        String url = sources[sourceIndex][1] + Uri.encode(currentQuery);'''
new_url = '''        String url = sources[sourceIndex][1] + Uri.encode(searchQuery);'''
if old_url not in s:
    raise SystemExit("V8 search URL injection point not found")
s = s.replace(old_url, new_url, 1)

old_js = '''                "function match(txt){var h=n(txt);if(!h)return false;var hits=0,words=0;for(var i=0;i<req.length;i++){var tok=req[i],has=h.indexOf(tok)>=0;if(/[0-9]/.test(tok)&&!has)return false;if(!/[0-9]/.test(tok)){words++;if(has)hits++;}}var need=words<=1?Math.min(words,1):Math.ceil(words*0.6);return hits>=need;}" +'''
new_js = '''                "function ed(a,b){var m=a.length,n2=b.length,prev=[],cur=[];for(var j=0;j<=n2;j++)prev[j]=j;for(var i=1;i<=m;i++){cur=[i];for(var j2=1;j2<=n2;j2++){var c=a.charAt(i-1)===b.charAt(j2-1)?0:1;cur[j2]=Math.min(cur[j2-1]+1,prev[j2]+1,prev[j2-1]+c);}prev=cur;}return prev[n2];}" +
                "function fuzzy(tok,h){if(h.indexOf(tok)>=0)return true;if(tok.length<4)return false;var ws=h.split(' '),mx=tok.length>=7?2:1;for(var fi=0;fi<ws.length;fi++){var w=ws[fi];if(w.length<3||Math.abs(tok.length-w.length)>mx)continue;if(ed(tok,w)<=mx)return true;}return false;}" +
                "function match(txt){var h=n(txt);if(!h)return false;var hits=0,words=0;for(var i=0;i<req.length;i++){var tok=req[i];if(/[0-9]/.test(tok)){if(h.indexOf(tok)<0)return false;}else{words++;if(fuzzy(tok,h))hits++;}}var need=words<=1?Math.min(words,1):Math.ceil(words*0.6);return hits>=need;}" +'''
if old_js not in s:
    raise SystemExit("V8 JS fuzzy-match injection point not found")
s = s.replace(old_js, new_js, 1)

old_java = '''    private boolean isLikelyMatch(String query, String text) {
        String q = normalize(query);
        String t = normalize(text);
        if (q.isEmpty() || t.isEmpty()) return false;
        String[] parts = q.split(" ");
        int wordCount = 0;
        int hit = 0;
        for (String p : parts) {
            if (p.isEmpty() || isStop(p)) continue;
            boolean has = t.contains(p);
            if (p.matches(".*\\\\d.*") && !has) return false;
            if (!p.matches(".*\\\\d.*")) {
                wordCount++;
                if (has) hit++;
            }
        }
        int need = wordCount <= 1 ? Math.min(wordCount, 1) : (int) Math.ceil(wordCount * 0.6);
        return hit >= need;
    }
'''

new_java = '''    private boolean isLikelyMatch(String query, String text) {
        String q = normalize(query);
        String t = normalize(text);
        if (q.isEmpty() || t.isEmpty()) return false;
        String[] parts = q.split(" ");
        int wordCount = 0;
        int hit = 0;
        for (String p : parts) {
            if (p.isEmpty() || isStop(p)) continue;
            if (p.matches(".*\\\\d.*")) {
                if (!t.contains(p)) return false;
            } else {
                wordCount++;
                if (tokenMatchesText(p, t)) hit++;
            }
        }
        int need = wordCount <= 1 ? Math.min(wordCount, 1) : (int) Math.ceil(wordCount * 0.6);
        return hit >= need;
    }

    private boolean tokenMatchesText(String token, String text) {
        if (text.contains(token)) return true;
        if (token.length() < 4) return false;
        int maxDistance = token.length() >= 7 ? 2 : 1;
        String[] words = text.split(" ");
        for (String word : words) {
            if (word.length() < 3 || Math.abs(token.length() - word.length()) > maxDistance) continue;
            if (levenshtein(token, word) <= maxDistance) return true;
        }
        return false;
    }

    private int levenshtein(String a, String b) {
        int[] previous = new int[b.length() + 1];
        int[] current = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[b.length()];
    }

    private String correctKnownBrands(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) return query;
        String[] brands = new String[]{
                "samsung", "apple", "xiaomi", "redmi", "poco", "huawei", "honor", "oppo", "vivo", "realme", "oneplus",
                "bosch", "philips", "siemens", "arçelik", "beko", "vestel", "dyson", "tefal", "karaca",
                "nike", "adidas", "puma", "reebok", "skechers", "newbalance",
                "lenovo", "asus", "acer", "dell", "msi", "canon", "epson", "sony", "jbl", "logitech",
                "makita", "dewalt", "stanley"
        };
        String[] tokens = normalized.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.length() < 4 || token.matches(".*\\\\d.*")) continue;
            String best = token;
            int bestDistance = Integer.MAX_VALUE;
            for (String brand : brands) {
                int maxDistance = Math.max(token.length(), brand.length()) >= 7 ? 2 : 1;
                if (Math.abs(token.length() - brand.length()) > maxDistance) continue;
                int d = levenshtein(token, brand);
                if (d <= maxDistance && d < bestDistance) {
                    bestDistance = d;
                    best = brand;
                }
            }
            tokens[i] = best;
        }
        StringBuilder out = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(token);
        }
        return out.length() == 0 ? query : out.toString();
    }
'''

if old_java not in s:
    raise SystemExit("V8 Java fuzzy-match injection point not found")
s = s.replace(old_java, new_java, 1)

main.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle")
g = gradle.read_text(encoding="utf-8")
g = g.replace("versionCode 7", "versionCode 8")
g = g.replace("versionName '0.7.0'", "versionName '0.8.0'")
gradle.write_text(g, encoding="utf-8")

print("Ucuzcu V8 typo tolerance and corrected search query applied")
