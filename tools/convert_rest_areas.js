// 졸음쉼터 자료 변환: 공공데이터포털 "전국졸음쉼터표준데이터"(15028203, 한국도로공사, 이용허락범위 제한 없음) CSV
//   → app/src/main/assets/content/rest_areas.json
// 사용: node tools/convert_rest_areas.js <내려받은 CSV> app/src/main/assets/content/rest_areas.json
//
// - 도로노선방향 "A기점 + B종점" 을 A → B 방향 차로의 쉼터로 읽는다 (자료에 공식 정의 문장이 없어 일반적인 해석을 따름).
// - routeHeading: 기점·종점 도시 중심(아래 표, 대략 좌표) 사이 방위각 — 방향을 정하는 데만 쓴다.
// - heading: 같은 노선의 이웃 쉼터(3~60km)로 구한 그 지점의 도로 축을 routeHeading 쪽으로 맞춘 값 (앱이 쓰는 진행 방향).
//   이웃이 없으면 routeHeading.
const fs = require('fs');
const [, , input, output] = process.argv;
if (!input || !output) { console.error('usage: node convert_rest_areas.js <csv> <json>'); process.exit(1); }
const text = fs.readFileSync(input, 'utf8').replace(/^﻿/, '');
function parse(t) { const rows = []; let row = [], f = '', q = false; for (let i = 0; i < t.length; i++) { const c = t[i]; if (q) { if (c == '"') { if (t[i + 1] == '"') { f += '"'; i++ } else q = false } else f += c } else if (c == '"') q = true; else if (c == ',') { row.push(f); f = '' } else if (c == '\n') { row.push(f); rows.push(row); row = []; f = '' } else if (c != '\r') f += c } if (f || row.length) { row.push(f); rows.push(row) } return rows }
const rows = parse(text).filter(r => r.length > 1);
const head = rows[0], col = n => head.indexOf(n);
const FIX = { '회턱': '회덕', '양앙': '양양', '양': '양평' }; // 원자료 오탈자
const P = { // 기점·종점 도시 대략 좌표 (방향 판단용)
  '서울': [37.566, 126.978], '부산': [35.180, 129.075], '통영': [34.854, 128.433], '하남': [37.539, 127.215], '춘천': [37.881, 127.730],
  '양평': [37.492, 127.488], '창원': [35.228, 128.681], '영덕': [36.415, 129.365], '청주': [36.642, 127.489], '제천': [37.133, 128.191],
  '평택': [36.992, 127.113], '양양': [38.075, 128.619], '강릉': [37.752, 128.876], '인천': [37.456, 126.705], '속초': [38.207, 128.592],
  '삼척': [37.450, 129.165], '판교': [37.394, 127.111], '일산': [37.658, 126.770], '퇴계원': [37.648, 127.144], '구리': [37.594, 127.130],
  '목포': [34.812, 126.392], '회덕': [36.376, 127.428], '논산': [36.187, 127.099], '서대전': [36.310, 127.350], '산내': [36.270, 127.460],
  '당진': [36.890, 126.628], '대전': [36.351, 127.385], '공주': [36.446, 127.119], '서천': [36.080, 126.692], '천안': [36.815, 127.114],
  '순천': [34.951, 127.488], '장수': [35.647, 127.521], '익산': [35.948, 126.958], '광주광역시': [35.160, 126.852], '대구': [35.871, 128.602],
  '담양': [35.321, 126.988], '고창': [35.436, 126.702], '울산': [35.539, 129.311], '언양': [35.564, 129.128], '산인': [35.300, 128.430],
  '기장': [35.244, 129.222], '동대구': [35.879, 128.628], '달서': [35.830, 128.533], '포항': [36.019, 129.343], '영암': [34.800, 126.697],
  '새만금': [35.800, 126.550], '포천': [37.895, 127.200], '세종': [36.480, 127.289], '양주': [37.785, 127.046], '파주': [37.760, 126.780],
};
const rad = d => d * Math.PI / 180, deg = r => r * 180 / Math.PI;
const dist = (a, b) => { const dLa = rad(b.lat - a.lat), dLo = rad(b.lng - a.lng); const h = Math.sin(dLa / 2) ** 2 + Math.cos(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.sin(dLo / 2) ** 2; return 2 * 6371000 * Math.asin(Math.sqrt(h)) };
const brg = (a, b) => { const y = Math.sin(rad(b.lng - a.lng)) * Math.cos(rad(b.lat)); const x = Math.cos(rad(a.lat)) * Math.sin(rad(b.lat)) - Math.sin(rad(a.lat)) * Math.cos(rad(b.lat)) * Math.cos(rad(b.lng - a.lng)); return (deg(Math.atan2(y, x)) + 360) % 360 };
const diff = (a, b) => Math.abs(((a - b) % 360 + 540) % 360 - 180);
const out = [], unknown = new Set();
let dataDate = '';
for (const r of rows.slice(1)) {
  const m = /^(.+?)기점\s*\+\s*(.+?)종점$/.exec(r[col('도로노선방향')].trim());
  if (!m) { unknown.add(r[col('도로노선방향')]); continue; }
  const from = FIX[m[1]] || m[1], to = FIX[m[2]] || m[2];
  if (!P[from] || !P[to]) { unknown.add(from + '/' + to); continue; }
  const lat = +r[col('위도')], lng = +r[col('경도')];
  if (!isFinite(lat) || !isFinite(lng)) continue;
  dataDate = r[col('데이터기준일자')] || dataDate;
  const routeHeading = brg({ lat: P[from][0], lng: P[from][1] }, { lat: P[to][0], lng: P[to][1] });
  out.push({ name: r[col('졸음쉼터명')].trim(), route: r[col('도로노선명')].trim(), from, to, lat: +lat.toFixed(6), lng: +lng.toFixed(6),
    heading: 0, routeHeading: Math.round(routeHeading), parking: parseInt(r[col('주차면수')]) || 0, toilet: r[col('화장실유무')].trim() === 'Y' });
}
let local = 0;
for (const s of out) {
  const near = out.filter(x => x !== s && x.route === s.route).map(x => ({ x, d: dist(s, x) })).filter(o => o.d >= 3000 && o.d <= 60000).sort((p, q) => p.d - q.d);
  let axis = null;
  if (near.length >= 2) { const b1 = brg(s, near[0].x), b2 = brg(s, near[1].x); axis = diff(b1, b2) > 90 ? brg(near[1].x, near[0].x) : b1; }
  else if (near.length === 1) axis = brg(s, near[0].x);
  if (axis !== null) { if (diff(axis, s.routeHeading) > 90) axis = (axis + 180) % 360; s.heading = Math.round(axis) % 360; local++; }
  else s.heading = s.routeHeading;
}
console.log('converted', out.length, 'of', rows.length - 1, '· local heading', local, '· unknown', [...unknown]);
fs.writeFileSync(output, JSON.stringify({
  version: 2,
  source: `공공데이터포털 전국졸음쉼터표준데이터(15028203, 한국도로공사) · 이용허락범위 제한 없음 · 데이터기준일자 ${dataDate}`,
  note: 'from→to: 원자료 도로노선방향("from기점 + to종점") — 그 방향 차로의 쉼터. heading: 같은 노선 이웃 쉼터로 구한 그 지점의 진행 방향(도, 북=0 시계방향), routeHeading: 기점→종점 도시 사이 방위각. 생성: tools/convert_rest_areas.js',
  restAreas: out,
}).replace(/\},\{/g, '},\n{'));
