// 휴게소 편의시설 자료 변환 → app/src/main/assets/content/service_areas.json
// 사용: node tools/convert_service_areas.js <serviceAreaAmenities.json> <가족사랑화장실.csv> app/src/main/assets/content/service_areas.json
//
// 입력 (둘 다 한국도로공사, 영리 목적 포함 자유 이용):
//  1) 고속도로 공공데이터 포털 LOD "휴게소별 편의시설 현황" — https://data.ex.co.kr/link/serviceAreaAmenities/download (JSON-LD)
//     휴게소명(label)·방향(direction)·편의시설(amenities: 수유실|수면실|샤워실…)·대표음식(repFoods)
//  2) 공공데이터포털 15142833 "한국도로공사_장애인 및 가족사랑 화장실 운영여부 및 위치" CSV(CP949) — 이용허락범위 제한 없음
//     휴게소명 "구정(속초)" 형식 + 가족사랑 화장실 운영여부
// 좌표는 어느 자료에도 없다 → 앱은 내비 TBT 의 경로 위 휴게소 이름으로 맞춘다. 같은 이름의 상·하행 중에는
// 방향 도시(dirLat/dirLng, 아래 대략 좌표) 쪽이 진행 방향과 맞는 것을 고른다.
const fs = require('fs');
const [, , lodPath, toiletPath, output] = process.argv;
if (!lodPath || !toiletPath || !output) { console.error('usage: node convert_service_areas.js <lod.json> <toilet.csv> <out.json>'); process.exit(1); }

const CITY = { // 방향 도시 대략 좌표 (방향 판단용)
  '서울': [37.566, 126.978], '부산': [35.180, 129.075], '목포': [34.812, 126.392], '현풍': [35.694, 128.447], '인천': [37.456, 126.705],
  '춘천': [37.881, 127.730], '음성': [36.940, 127.690], '시흥': [37.380, 126.803], '대구': [35.871, 128.602], '익산': [35.948, 126.958],
  '순천': [34.951, 127.488], '청원': [36.642, 127.489], '상주': [36.411, 128.159], '논산': [36.187, 127.099], '하남': [37.539, 127.215],
  '통영': [34.854, 128.433], '양평': [37.492, 127.488], '강릉': [37.752, 128.876], '공주': [36.446, 127.119], '전주': [35.824, 127.148],
  '포항': [36.019, 129.343], '냉정': [35.238, 128.795], '마산': [35.214, 128.582], '무안': [34.990, 126.481], '평택': [36.992, 127.113],
  '대전': [36.351, 127.385], '동해': [37.525, 129.114], '속초': [38.207, 128.592], '광양': [34.940, 127.696], '서천': [36.080, 126.692],
  '삼척': [37.450, 129.165], '광주': [35.160, 126.852], '창원': [35.228, 128.681], '당진': [36.890, 126.628], '영덕': [36.415, 129.365],
  '천안': [36.815, 127.114], '청주': [36.642, 127.489], '진주': [35.180, 128.108], '김해': [35.229, 128.889], '울산': [35.539, 129.311],
  '안산': [37.322, 126.831], '여주': [37.298, 127.637], '원주': [37.342, 127.920], '제천': [37.133, 128.191], '함양': [35.520, 127.725],
  '고창': [35.436, 126.702], '담양': [35.321, 126.988], '영암': [34.800, 126.697], '구리': [37.594, 127.130], '일산': [37.658, 126.770],
  '판교': [37.394, 127.111], '퇴계원': [37.648, 127.144], '양양': [38.075, 128.619], '세종': [36.480, 127.289], '포천': [37.895, 127.200],
  '김천': [36.140, 128.114], '장수': [35.647, 127.521], '새만금': [35.800, 126.550], '아산': [36.790, 127.002], '서산': [36.785, 126.450],
  '평창': [37.370, 128.390], '구미': [36.120, 128.344], '경주': [35.856, 129.225], '대소': [36.976, 127.464], '남원': [35.416, 127.390],
  '남이': [36.560, 127.430], '서부산': [35.160, 128.980], '완주': [35.904, 127.162],
  // '통합' = 양방향이 함께 쓰는 휴게소 → 좌표 없이 둔다(앱은 방향과 상관없이 맞는 것으로 본다)
};
const NAME_FIX = { '하남(만남)': '하남만남의광장' }; // 원자료 줄임 표기 → 휴게소 이름 (내비 TBT 와 맞추기)

// 1) LOD JSON-LD
const lod = JSON.parse(fs.readFileSync(lodPath, 'utf8'));
const D = 'http://data.ex.co.kr:80/link/def/';
const val = (o, k) => (o[D + k]?.[0]?.value ?? o['http://www.w3.org/2000/01/rdf-schema#' + k]?.[0]?.value ?? '').trim();
const key = (name, dir) => `${name}|${dir}`;
const areas = new Map();
for (const o of Object.values(lod)) {
  const name = NAME_FIX[val(o, 'label')] || val(o, 'label');
  const dir = val(o, 'direction');
  if (!name || !dir) continue;
  const amenities = [...new Set(val(o, 'amenities').split('|').map(s => s.trim()).filter(Boolean))];
  areas.set(key(name, dir), { name, direction: dir, amenities, foods: val(o, 'repFoods') || null, familyRestroom: null });
}

// 2) 가족사랑 화장실 CSV (CP949)
const csvText = new TextDecoder('euc-kr').decode(fs.readFileSync(toiletPath));
const rows = csvText.split(/\r?\n/).filter(l => l.trim()).map(l => l.split(','));
const head = rows[0].map(h => h.trim());
const col = n => head.findIndex(h => h.startsWith(n));
const cName = col('휴게소명');
const famCols = head.map((h, i) => (/가족사랑화장실.*운영여부/.test(h) ? i : -1)).filter(i => i >= 0);
let matched = 0, added = 0;
for (const r of rows.slice(1)) {
  const m = /^(.+?)\((.+?)\)$/.exec((r[cName] || '').trim());
  if (!m) continue;
  const name = NAME_FIX[m[1]] || m[1], dir = m[2];
  const family = famCols.some(i => (r[i] || '').trim().toUpperCase() === 'Y');
  const k = key(name, dir);
  if (areas.has(k)) { areas.get(k).familyRestroom = family; matched++; }
  else { areas.set(k, { name, direction: dir, amenities: [], foods: null, familyRestroom: family }); added++; }
}

// 같은 차로를 자료마다 다른 방향 도시로 적은 경우(예: 편의시설 "충주(마산)" / 화장실 "충주(창원)") — 방향 도시가 40 km 안이면 합친다
const km = (a, b) => { const r = d => d * Math.PI / 180; const dLa = r(b[0] - a[0]), dLo = r(b[1] - a[1]); const h = Math.sin(dLa / 2) ** 2 + Math.cos(r(a[0])) * Math.cos(r(b[0])) * Math.sin(dLo / 2) ** 2; return 2 * 6371 * Math.asin(Math.sqrt(h)) };
let mergedDirs = 0;
for (const [k, a] of [...areas.entries()]) {
  if (a.amenities.length || a.foods) continue; // 화장실 자료에만 있는 항목만 합칠 대상
  const ca = CITY[a.direction];
  if (!ca) continue;
  const twin = [...areas.values()].find(b => b !== a && b.name === a.name && (b.amenities.length || b.foods) && CITY[b.direction] && km(ca, CITY[b.direction]) < 40);
  if (twin) { if (twin.familyRestroom == null) twin.familyRestroom = a.familyRestroom; areas.delete(k); mergedDirs++; }
}

const unknownDirs = new Set();
const out = [...areas.values()].map(a => {
  const c = CITY[a.direction];
  if (!c) unknownDirs.add(a.direction);
  return { ...a, dirLat: c ? c[0] : null, dirLng: c ? c[1] : null };
}).sort((a, b) => a.name.localeCompare(b.name, 'ko') || a.direction.localeCompare(b.direction, 'ko'));

console.log('service areas', out.length, '· LOD', out.filter(a => a.amenities.length || a.foods).length, '· 화장실 자료 맞춤', matched, '· 화장실 자료에만', added, '· 방향 이름만 달라 합침', mergedDirs, '· 방향 좌표 없음', [...unknownDirs]);
fs.writeFileSync(output, JSON.stringify({
  version: 1,
  source: '한국도로공사 고속도로 공공데이터 포털 LOD "휴게소별 편의시설 현황"(영리 포함 자유 이용) + 공공데이터포털 15142833 "장애인 및 가족사랑 화장실 운영여부 및 위치"(이용허락범위 제한 없음)',
  note: '좌표 없음 — 내비 TBT 의 경로 위 휴게소 이름으로 맞춘다. direction: 휴게소 방향(그 도시 쪽 차로), dirLat/dirLng: 그 도시 대략 좌표(상·하행 고르기용). 생성: tools/convert_service_areas.js',
  serviceAreas: out,
}, null, 0).replace(/\},\{/g, '},\n{'));
