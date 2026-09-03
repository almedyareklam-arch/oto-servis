from pathlib import Path

main = Path("app/src/main/java/com/ucuzcu/app/MainActivity.java")
s = main.read_text(encoding="utf-8")

s = s.replace("Ucuzcu/0.6", "Ucuzcu/0.7")

old_js = '''                "var req=toks.filter(function(x){return !stop[x];});if(!req.length)req=toks;" +
                "function match(txt){var h=n(txt);if(!h)return false;var hits=0,words=0;for(var i=0;i<req.length;i++){var tok=req[i],has=h.indexOf(tok)>=0;if(/[0-9]/.test(tok)&&!has)return false;if(!/[0-9]/.test(tok)){words++;if(has)hits++;}}var need=words<=1?Math.min(words,1):Math.ceil(words*0.6);return hits>=need;}" +'''

new_js = '''                "var req=toks.filter(function(x){return !stop[x];});if(!req.length)req=toks;" +
                "var accessoryTerms=['kılıf','kilif','kapak','magsafe','ekran koruyucu','koruyucu cam','kamera koruyucu','lens koruyucu','şarj cihazı','sarj cihazi','şarj aleti','sarj aleti','adaptör','adaptor','adapter','kablo','powerbank','tutucu','holder','telefon standı','telefon standi','askı','aski','kordon','skin','sticker','tabanlık','tabanlik','bağcık','bagcik'];" +
                "var queryWantsAccessory=accessoryTerms.some(function(x){return raw.indexOf(x)>=0;});" +
                "function accessoryMismatch(txt){if(queryWantsAccessory)return false;var h=n(txt),hits=0;for(var ai=0;ai<accessoryTerms.length;ai++){if(h.indexOf(accessoryTerms[ai])>=0)hits++;}if(!hits)return false;var relation=h.indexOf('uyumlu')>=0||h.indexOf('için')>=0||h.indexOf('icin')>=0||h.indexOf('compatible')>=0||h.indexOf('aksesuar')>=0;var strongMain=h.indexOf('akıllı telefon')>=0||h.indexOf('akilli telefon')>=0||h.indexOf('cep telefonu')>=0||h.indexOf('smartphone')>=0||h.indexOf(' gb')>=0;if(strongMain&&!relation&&hits<2)return false;return relation||hits>=2||h.indexOf('kılıf')>=0||h.indexOf('kilif')>=0||h.indexOf('kapak')>=0||h.indexOf('magsafe')>=0;}" +
                "function match(txt){var h=n(txt);if(!h)return false;var hits=0,words=0;for(var i=0;i<req.length;i++){var tok=req[i],has=h.indexOf(tok)>=0;if(/[0-9]/.test(tok)&&!has)return false;if(!/[0-9]/.test(tok)){words++;if(has)hits++;}}var need=words<=1?Math.min(words,1):Math.ceil(words*0.6);return hits>=need;}" +'''

if old_js not in s:
    raise SystemExit("V7 JS injection point not found")
s = s.replace(old_js, new_js, 1)

old_card = '''                "var links=Array.prototype.slice.call(card.querySelectorAll('a[href]'));if(!title){for(var a=0;a<links.length;a++){var at=clean(links[a].innerText);if(at.length>4&&at.length<220){title=at;break;}}}if(!title)title=txt.substring(0,180);if(!match(title+' '+txt.substring(0,500)))return;" +'''
new_card = '''                "var links=Array.prototype.slice.call(card.querySelectorAll('a[href]'));if(!title){for(var a=0;a<links.length;a++){var at=clean(links[a].innerText);if(at.length>4&&at.length<220){title=at;break;}}}if(!title)title=txt.substring(0,180);if(!match(title+' '+txt.substring(0,500)))return;if(accessoryMismatch(title))return;" +'''
if old_card not in s:
    raise SystemExit("V7 product-card injection point not found")
s = s.replace(old_card, new_card, 1)

old_clean = '''            if (o == null || o.price <= 0 || !isLikelyMatch(currentQuery, o.title + " " + o.detail)) continue;'''
new_clean = '''            if (o == null || o.price <= 0 || !isLikelyMatch(currentQuery, o.title + " " + o.detail) || isAccessoryMismatch(currentQuery, o.title + " " + o.detail)) continue;'''
if old_clean not in s:
    raise SystemExit("V7 Java filter injection point not found")
s = s.replace(old_clean, new_clean, 1)

needle = '''    private boolean isStop(String p) {'''
helper = '''    private boolean isAccessoryMismatch(String query, String text) {
        String q = normalize(query);
        String t = normalize(text);
        if (q.isEmpty() || t.isEmpty()) return false;

        String[] accessoryTerms = new String[]{
                "kılıf", "kilif", "kapak", "magsafe", "ekran koruyucu", "koruyucu cam", "kamera koruyucu",
                "lens koruyucu", "şarj cihazı", "sarj cihazi", "şarj aleti", "sarj aleti", "adaptör", "adaptor",
                "adapter", "kablo", "powerbank", "tutucu", "holder", "telefon standı", "telefon standi", "askı",
                "aski", "kordon", "skin", "sticker", "tabanlık", "tabanlik", "bağcık", "bagcik"
        };

        boolean queryWantsAccessory = false;
        for (String term : accessoryTerms) {
            if (q.contains(term)) {
                queryWantsAccessory = true;
                break;
            }
        }
        if (queryWantsAccessory) return false;

        int hits = 0;
        for (String term : accessoryTerms) {
            if (t.contains(term)) hits++;
        }
        if (hits == 0) return false;

        boolean relation = t.contains("uyumlu") || t.contains("için") || t.contains("icin") ||
                t.contains("compatible") || t.contains("aksesuar");
        boolean strongMain = t.contains("akıllı telefon") || t.contains("akilli telefon") ||
                t.contains("cep telefonu") || t.contains("smartphone") || t.contains(" gb");

        if (strongMain && !relation && hits < 2) return false;
        return relation || hits >= 2 || t.contains("kılıf") || t.contains("kilif") ||
                t.contains("kapak") || t.contains("magsafe");
    }

'''
if needle not in s:
    raise SystemExit("V7 Java helper injection point not found")
s = s.replace(needle, helper + needle, 1)

main.write_text(s, encoding="utf-8")

gradle = Path("app/build.gradle")
g = gradle.read_text(encoding="utf-8")
g = g.replace("versionCode 6", "versionCode 7")
g = g.replace("versionName '0.6.0'", "versionName '0.7.0'")
gradle.write_text(g, encoding="utf-8")

print("Ucuzcu V7 intent/accessory filter applied")
