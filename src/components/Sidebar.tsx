'use client';
import Link from 'next/link';
import {usePathname} from 'next/navigation';
const items=[['Dashboard','/'],['Servis Kabul','/servis-kabul'],['İş Emirleri','/is-emirleri'],['Müşteriler','/musteriler'],['Araçlar','/araclar'],['Randevular','/randevular'],['Stok / Yedek Parça','/stok'],['Teklifler','/teklifler'],['Faturalar','/faturalar'],['Kasa & Cari','/kasa-cari'],['Tedarikçiler','/tedarikciler'],['Raporlar','/raporlar'],['Personel / Ustalar','/personel'],['Ayarlar','/ayarlar']];
export function Sidebar(){const p=usePathname();return <aside className="sidebar"><div className="brand"><span>OTO</span> SERVİS<small>YÖNETİM SİSTEMİ</small></div><nav className="nav">{items.map(([x,h])=><Link className={p===h?'active':''} href={h} key={h}>{x}</Link>)}</nav><div className="sidefoot"><b>Demo Şube</b><small>Merkez • Adana</small></div></aside>}
