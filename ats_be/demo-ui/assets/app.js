// Phần dùng chung cho các trang demo: đăng nhập Keycloak, gọi API qua API Gateway, tạo CV mẫu.
const ATS = (() => {
  const GATEWAY = 'http://localhost:8080';
  const TOKEN_URL = 'http://localhost:8180/realms/ats/protocol/openid-connect/token';
  const CLIENT_ID = 'ats-web';            // client public của Keycloak, webOrigins = http://localhost:3000
  const STORAGE_KEY = 'ats-demo-auth';

  // ---------------------------------------------------------------- đăng nhập
  const readAuth = () => { try { return JSON.parse(localStorage.getItem(STORAGE_KEY)); } catch { return null; } };
  const writeAuth = (auth) => { try { localStorage.setItem(STORAGE_KEY, JSON.stringify(auth)); } catch { /* bỏ qua */ } };

  function decodeJwt(token) {
    const part = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    return JSON.parse(decodeURIComponent(escape(atob(part))));
  }

  async function requestToken(params) {
    const res = await fetch(TOKEN_URL, {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams({ client_id: CLIENT_ID, ...params })
    });
    const body = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(body.error_description || 'Đăng nhập thất bại');
    const now = Date.now();
    const claims = decodeJwt(body.access_token);
    const auth = {
      access: body.access_token,
      refresh: body.refresh_token,
      exp: now + body.expires_in * 1000,
      refreshExp: now + (body.refresh_expires_in || 1800) * 1000,
      username: claims.preferred_username,
      roles: (claims.realm_access && claims.realm_access.roles || []).filter(r => /^[A-Z_]+$/.test(r))
    };
    writeAuth(auth);
    return auth;
  }

  let refreshing = null;   // nhiều request cùng lúc chỉ làm mới token 1 lần

  async function accessToken() {
    const auth = readAuth();
    if (!auth) throw new Error('NOT_LOGGED_IN');
    if (Date.now() < auth.exp - 30000) return auth.access;
    if (Date.now() > auth.refreshExp - 5000) { logout(); throw new Error('NOT_LOGGED_IN'); }
    refreshing = refreshing || requestToken({ grant_type: 'refresh_token', refresh_token: auth.refresh })
      .finally(() => { refreshing = null; });
    return (await refreshing).access;
  }

  const login = (username, password) => requestToken({ grant_type: 'password', username, password });
  const logout = () => { try { localStorage.removeItem(STORAGE_KEY); } catch { /* bỏ qua */ } };
  const currentUser = () => readAuth();

  async function api(path, options = {}) {
    const token = await accessToken();
    return fetch(GATEWAY + path, {
      ...options,
      headers: { ...(options.headers || {}), Authorization: 'Bearer ' + token }
    });
  }

  // ---------------------------------------------------------------- khung trang
  const PAGES = [
    { href: 'tiep-nhan.html', label: 'Tiếp nhận hồ sơ' },
    { href: 'hr.html', label: 'HR chấm điểm CV' },
    { href: 'giam-sat.html', label: 'Giám sát bulkhead' }
  ];

  function renderTopbar(active) {
    const user = currentUser();
    const el = document.getElementById('topbar');
    el.innerHTML = `
      <a class="brand" href="index.html"><span class="logo">ATS</span> Tuyển dụng</a>
      <nav>${PAGES.map(p => `<a href="${p.href}" class="${p.href === active ? 'active' : ''}">${p.label}</a>`).join('')}</nav>
      <div class="who">${user ? `<span>${escapeHtml(user.username)} · ${user.roles.join(', ')}</span>
        <button class="link" id="logout">Đăng xuất</button>` : ''}</div>`;
    const btn = document.getElementById('logout');
    if (btn) btn.onclick = () => { logout(); location.href = 'index.html'; };
  }

  /** Chưa đăng nhập -> quay về trang đăng nhập. */
  function requireLogin() {
    if (!currentUser()) { location.href = 'index.html?next=' + encodeURIComponent(location.pathname.split('/').pop()); return false; }
    return true;
  }

  // ---------------------------------------------------------------- CV mẫu & upload
  const LAST = ['Nguyễn', 'Trần', 'Lê', 'Phạm', 'Hoàng', 'Vũ', 'Đặng', 'Bùi', 'Đỗ', 'Hồ'];
  const MID = ['Văn', 'Thị', 'Minh', 'Quang', 'Thu', 'Đức', 'Ngọc', 'Hải'];
  const FIRST = ['An', 'Bình', 'Châu', 'Dũng', 'Giang', 'Hà', 'Khoa', 'Linh', 'Nam', 'Phúc', 'Quân', 'Trang', 'Vy'];
  const SKILLS = ['Java', 'Spring Boot', 'Spring Cloud', 'React', 'JavaScript', 'TypeScript', 'Python', 'SQL',
    'PostgreSQL', 'Docker', 'Kubernetes', 'AWS', 'Git', 'Kafka', 'Redis'];
  const pick = (arr) => arr[Math.floor(Math.random() * arr.length)];

  function sampleCandidate(index) {
    const fullName = `${pick(LAST)} ${pick(MID)} ${pick(FIRST)}`;
    const tag = `${Date.now().toString(36)}${index}${Math.floor(Math.random() * 1000)}`;
    const skills = [...SKILLS].sort(() => Math.random() - 0.5).slice(0, 4 + Math.floor(Math.random() * 4));
    const years = 1 + Math.floor(Math.random() * 8);
    const phone = '09' + String(Math.floor(Math.random() * 1e8)).padStart(8, '0');
    const email = `ungvien.${tag}@example.com`;
    const text = [
      fullName, `Email: ${email}`, `Phone: ${phone}`,
      `Backend Developer - ${years} years experience`,
      `Skills: ${skills.join(', ')}`,
      `Education: Bachelor - University of Science`,
      `Languages: English, Vietnamese`
    ].join('\n');
    return { fullName, email, phone, text, fileName: `cv-${tag}.txt` };
  }

  /** Gửi 1 hồ sơ kèm CV lên candidate-service (multipart) qua API Gateway. */
  async function uploadCv({ fullName, email, phone, file, fileName }) {
    const form = new FormData();
    form.append('fullName', fullName);
    form.append('email', email);
    if (phone) form.append('phone', phone);
    form.append('cvFile', file, fileName || file.name);
    const started = performance.now();
    const res = await api('/api/v1/candidates', { method: 'POST', body: form });
    const body = await res.json().catch(() => ({}));
    return { ok: res.ok, status: res.status, ms: performance.now() - started, body };
  }

  /** Mô phỏng đợt cao điểm: n ứng viên nộp CV, tối đa `concurrency` request song song. */
  async function simulateUploads(n, { concurrency = 6, onProgress } = {}) {
    let next = 0, done = 0, failed = 0;
    const started = performance.now();
    async function worker() {
      while (next < n) {
        const c = sampleCandidate(next++);
        const res = await uploadCv({ ...c, file: new Blob([c.text], { type: 'text/plain' }) }).catch(() => ({ ok: false }));
        done++; if (!res.ok) failed++;
        if (onProgress) onProgress({ done, failed, total: n });
      }
    }
    await Promise.all(Array.from({ length: Math.min(concurrency, n) }, worker));
    return { done, failed, ms: performance.now() - started };
  }

  // ---------------------------------------------------------------- test ngăn Semaphore
  const BURST_BODY = JSON.stringify({
    cvText: 'Backend Developer 4 years experience. Skills: Java, Spring Boot, PostgreSQL, Docker',
    jobDescription: 'Java, Spring Boot, PostgreSQL, Docker, Kubernetes'
  });

  /** n HR bấm "Chấm điểm" CÙNG LÚC: gửi n request /match song song, trả về kết quả từng request. */
  function burstMatch(n, body = BURST_BODY) {
    return Promise.all(Array.from({ length: n }, async (_, index) => {
      const started = performance.now();
      try {
        const res = await api('/api/v1/cvparse/match', {
          method: 'POST', headers: { 'Content-Type': 'application/json' }, body
        });
        await res.text();
        return { hr: index + 1, status: res.status, ms: performance.now() - started };
      } catch {
        return { hr: index + 1, status: 0, ms: performance.now() - started };
      }
    }));
  }

  /** Số vé Semaphore đang bị giữ ngay lúc này (để biết trước khi test đã có HR khác cầm vé chưa). */
  async function ticketsHeld() {
    try {
      const s = await (await api('/api/v1/cvparse/bulkhead')).json();
      const i = s.interactiveBulkhead;
      return { enabled: s.bulkheadEnabled, held: i.maxConcurrentCalls - i.availableConcurrentCalls, max: i.maxConcurrentCalls };
    } catch { return null; }
  }

  /** Vẽ kết quả burstMatch: mỗi HR một ô (xanh = 200, cam = 503 bị Semaphore từ chối). */
  function renderBurst(el, results, before = null) {
    const ok = results.filter(r => r.status === 200);
    const busy = results.filter(r => r.status === 503);
    const range = (list) => list.length ? `${fmtMs(Math.min(...list.map(r => r.ms)))}–${fmtMs(Math.max(...list.map(r => r.ms)))}` : '–';
    el.innerHTML = `
      <div class="row" style="margin-bottom:10px">${results.sort((a, b) => a.ms - b.ms).map(r => `
        <div class="kpi-cell ${r.status === 200 ? 'ok' : r.status === 503 ? 'busy' : 'err'}">
          <b>HR-${r.hr}</b><span>${r.status === 200 ? '200 · được chấm' : r.status === 503 ? '503 · bị từ chối' : 'lỗi ' + r.status}</span><span>${fmtMs(r.ms)}</span>
        </div>`).join('')}</div>
      <div class="notice ${busy.length ? 'warn' : 'ok'}">
        <b>${ok.length}</b> HR được chấm điểm (${range(ok)}) · <b>${busy.length}</b> HR bị từ chối ngay với 503 "thử lại sau" (${range(busy)}).
        ${busy.length ? 'Ngăn Semaphore chỉ cho 2 HR chạy cùng lúc, người tới sau chờ tối đa 0,5 giây rồi bị từ chối thay vì treo.' : 'Không ai bị từ chối – bulkhead đang tắt hoặc số HR ≤ 2.'}
        ${before && before.enabled && before.held > 0
          ? `<br><b>Lúc bấm đã có ${before.held}/${before.max} vé đang bị HR khác cầm</b> (tab HR/Giám sát khác đang mở, hoặc "Đo liên tục") → lượt này chỉ còn ${before.max - before.held} vé.`
          : busy.length && ok.length < 2 ? '<br>Ít hơn 2 HR được chấm vì có HR khác (tab khác) lấy vé đúng lúc bấm.' : ''}
      </div>`;
  }

  // ---------------------------------------------------------------- tiện ích
  const sleep = (ms) => new Promise(r => setTimeout(r, ms));
  const escapeHtml = (s) => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const fmtMs = (ms) => ms < 1000 ? `${Math.round(ms)} ms` : `${(ms / 1000).toFixed(1)} s`;
  const fmtClock = (iso) => iso ? new Date(iso).toLocaleTimeString('vi-VN') : '–';
  const STATUS = {
    PENDING: { label: 'Đang phân tích', cls: 'pending' },
    PARSED: { label: 'Đã phân tích', cls: 'ok' },
    FAILED: { label: 'Lỗi', cls: 'bad' }
  };
  const statusBadge = (s) => { const x = STATUS[s] || { label: s, cls: '' }; return `<span class="badge ${x.cls}">${x.label}</span>`; };

  /** Dựng nội dung CV (dạng văn bản) từ kết quả parse, để HR chấm điểm với JD. */
  function cvTextFromParsed(cv) {
    const d = cv.parsedData || {};
    return [cv.candidateName, `Email: ${d.email || cv.candidateEmail}`,
      `${d.yearsOfExperience ?? ''} years experience`, `Skills: ${(d.skills || []).join(', ')}`,
      d.education || '', `Languages: ${(d.languages || []).join(', ')}`].join('\n');
  }

  return {
    login, logout, currentUser, api, renderTopbar, requireLogin,
    sampleCandidate, uploadCv, simulateUploads, cvTextFromParsed, burstMatch, renderBurst, ticketsHeld,
    sleep, escapeHtml, fmtMs, fmtClock, statusBadge
  };
})();
