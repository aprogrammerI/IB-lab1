// simple frontend that relies on cookie set by server (HttpOnly)
async function postJson(url, data) {
    const res = await fetch(url, { method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(data) });
    const text = await res.text();
    return { ok: res.ok, text, status: res.status };
}

document.getElementById('btnRegister').addEventListener('click', async () => {
    const username = document.getElementById('r_username').value;
    const email = document.getElementById('r_email').value;
    const password = document.getElementById('r_password').value;
    const res = await postJson('/register', { username, email, password });
    document.getElementById('regMsg').textContent = res.ok ? res.text : 'Error: ' + res.text;
});

document.getElementById('btnLogin').addEventListener('click', async () => {
    const username = document.getElementById('l_username').value;
    const password = document.getElementById('l_password').value;
    const res = await postJson('/login', { username, password });
    const msg = document.getElementById('loginMsg');
    if (res.ok) {
        msg.textContent = 'Login ok (cookie set)';
        await loadProfile(); // cookie sent by browser automatically
        document.getElementById('login').style.display = 'none';
    } else {
        msg.textContent = 'Login failed: ' + res.text;
    }
});

async function loadProfile() {
    const res = await fetch('/user/me');
    if (!res.ok) {
        return;
    }
    const data = await res.json();
    document.getElementById('profileInfo').textContent = `Username: ${data.username}, Email: ${data.email}`;
    document.getElementById('profile').style.display = 'block';
    document.getElementById('notes').style.display = 'block';
    document.getElementById('register').style.display = 'none';
}

document.getElementById('btnLogout').addEventListener('click', async () => {
    await fetch('/logout', { method: 'POST' });
    document.getElementById('profile').style.display = 'none';
    document.getElementById('notes').style.display = 'none';
    document.getElementById('login').style.display = 'block';
});

document.getElementById('btnSave').addEventListener('click', async () => {
    const note = document.getElementById('note').value;
    const res = await postJson('/user/note', { note });
    document.getElementById('noteMsg').textContent = res.ok ? 'Saved' : 'Error: ' + res.text;
});

document.getElementById('btnLoad').addEventListener('click', async () => {
    const res = await fetch('/user/note');
    if (!res.ok) {
        document.getElementById('noteMsg').textContent = 'Error: ' + await res.text();
        return;
    }
    const data = await res.json();
    document.getElementById('note').value = data.note || '';
    document.getElementById('noteMsg').textContent = 'Loaded';
});

// on load, try to show profile if cookie present
window.addEventListener('load', loadProfile);
