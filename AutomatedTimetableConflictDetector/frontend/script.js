// Connect to Java backend on port 8080
const API_BASE =
    window.location.protocol === "file:" || window.location.port !== "8080"
        ? "http://localhost:8080/api"
        : "/api";

const DAYS = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];
let allEntries = [];

document.addEventListener("DOMContentLoaded", () => {
    document.getElementById("currentDate").textContent = new Date().toLocaleDateString();
    setupNavigation();
    setupActions();
    loadDashboardData();
});

// Switch pages via sidebar buttons or quick action buttons
function setupNavigation() {
    document.querySelectorAll("[data-page]").forEach((btn) => {
        btn.addEventListener("click", () => openPage(btn.dataset.page));
    });
    document.querySelectorAll("[data-goto]").forEach((btn) => {
        btn.addEventListener("click", () => openPage(btn.dataset.goto));
    });
    document.getElementById("menuBtn").addEventListener("click", () => {
        document.getElementById("sidebar").classList.toggle("open");
    });
}

function openPage(pageName) {
    document.querySelectorAll(".nav-btn").forEach((b) => b.classList.toggle("active", b.dataset.page === pageName));
    document.querySelectorAll(".page").forEach((p) => p.classList.toggle("active", p.id === `page-${pageName}`));
    document.getElementById("sidebar").classList.remove("open");
}

function setupActions() {
    document.getElementById("entryForm").addEventListener("submit", addEntry);
    document.getElementById("resetBtn").addEventListener("click", () => {
        document.getElementById("entryForm").reset();
        document.getElementById("formError").classList.add("hidden");
    });

    document.getElementById("refreshBtn").addEventListener("click", loadDashboardData);
    document.getElementById("quickCheckBtn").addEventListener("click", () => {
        openPage("conflicts");
        detectConflicts("all");
    });

    document.querySelectorAll("[data-scope]").forEach((btn) => {
        btn.addEventListener("click", () => detectConflicts(btn.dataset.scope));
    });
}

// Reusable fetch helper for Java REST API calls
async function callApi(path, options = {}) {
    const loader = document.getElementById("loader");
    loader.classList.remove("hidden");
    try {
        const res = await fetch(`${API_BASE}${path}`, {
            headers: { "Content-Type": "application/json" },
            ...options
        });
        const data = await res.json();
        if (!res.ok || data.success === false) {
            throw new Error(data.error || "Request failed");
        }
        setBackendStatus(true);
        return data;
    } catch (err) {
        if (err instanceof TypeError) {
            setBackendStatus(false);
            throw new Error("Backend offline. Start Main.java on port 8080.");
        }
        throw err;
    } finally {
        loader.classList.add("hidden");
    }
}

function setBackendStatus(online) {
    const badge = document.getElementById("statusBadge");
    badge.className = `pill ${online ? "status-online" : "status-offline"}`;
    badge.querySelector(".status-label").textContent = online ? "Backend Online" : "Backend Offline";
}

// Load timetable entries and stats from Java
async function loadDashboardData() {
    try {
        const data = await callApi("/timetable");
        allEntries = data.entries || [];
        const stats = data.stats || {};

        document.getElementById("statEntries").textContent = stats.totalEntries ?? allEntries.length;
        document.getElementById("statFaculty").textContent = stats.facultyConflicts ?? 0;
        document.getElementById("statRoom").textContent = stats.roomConflicts ?? 0;
        document.getElementById("statSection").textContent = stats.sectionConflicts ?? 0;

        renderSummary(stats);
        renderRecentEntries();
        renderTable();
        renderWeeklyGrid("dashboardGrid");
        renderWeeklyGrid("fullWeeklyGrid");
    } catch (err) {
        showToast(err.message, "err");
        document.getElementById("dashboardSummary").innerHTML =
            `<div class="alert alert-red">${escapeHtml(err.message)}</div>`;
    }
}

function renderSummary(stats) {
    const box = document.getElementById("dashboardSummary");
    const total = (stats.facultyConflicts || 0) + (stats.roomConflicts || 0) + (stats.sectionConflicts || 0);

    if (allEntries.length === 0) {
        box.innerHTML = `<div class="alert alert-orange">Timetable is empty. Add entries to run conflict checks.</div>`;
    } else if (total === 0) {
        box.innerHTML = `<div class="alert alert-green">No scheduling conflicts found. Your timetable is valid!</div>`;
    } else {
        box.innerHTML = `<div class="alert alert-red">${total} conflict(s) found — Faculty: ${stats.facultyConflicts}, Room: ${stats.roomConflicts}, Section: ${stats.sectionConflicts}</div>`;
    }
}

function renderRecentEntries() {
    const box = document.getElementById("recentEntries");
    if (!allEntries.length) {
        box.innerHTML = `<p class="muted">No entries yet.</p>`;
        return;
    }
    const recent = [...allEntries].slice(-4).reverse();
    box.innerHTML = recent.map((e) => `
        <div class="recent-row">
            <div><strong>${escapeHtml(e.subject)}</strong> (${escapeHtml(e.section)})<br><small class="muted">${escapeHtml(e.day)} • ${escapeHtml(e.faculty)} • ${escapeHtml(e.room)}</small></div>
            <span>${escapeHtml(e.startTime)} - ${escapeHtml(e.endTime)}</span>
        </div>
    `).join("");
}

function renderTable() {
    const tbody = document.getElementById("entriesTableBody");
    document.getElementById("entryCount").textContent = allEntries.length;

    if (!allEntries.length) {
        tbody.innerHTML = `<tr><td colspan="9" class="center">No timetable entries available.</td></tr>`;
        return;
    }

    tbody.innerHTML = allEntries.map((e) => `
        <tr>
            <td>#${e.id}</td>
            <td>${escapeHtml(e.day)}</td>
            <td><strong>${escapeHtml(e.subject)}</strong></td>
            <td>${escapeHtml(e.faculty)}</td>
            <td>${escapeHtml(e.section)}</td>
            <td>${escapeHtml(e.room)}</td>
            <td>${escapeHtml(e.startTime)}</td>
            <td>${escapeHtml(e.endTime)}</td>
            <td><button class="btn btn-delete" onclick="deleteEntry(${e.id})">Delete</button></td>
        </tr>
    `).join("");
}

// Validate form and POST to Java backend
async function addEntry(event) {
    event.preventDefault();
    const errBox = document.getElementById("formError");
    errBox.classList.add("hidden");

    const entry = {
        day: document.getElementById("day").value.trim(),
        subject: document.getElementById("subject").value.trim(),
        faculty: document.getElementById("faculty").value.trim(),
        section: document.getElementById("section").value.trim(),
        room: document.getElementById("room").value.trim(),
        startTime: document.getElementById("startTime").value.trim(),
        endTime: document.getElementById("endTime").value.trim()
    };

    if (!entry.day || !entry.subject || !entry.faculty || !entry.section || !entry.room || !entry.startTime || !entry.endTime) {
        errBox.textContent = "Please fill in all fields.";
        errBox.classList.remove("hidden");
        return;
    }
    if (entry.startTime >= entry.endTime) {
        errBox.textContent = "Invalid time range: Start Time must be earlier than End Time.";
        errBox.classList.remove("hidden");
        return;
    }

    try {
        const res = await callApi("/timetable", { method: "POST", body: JSON.stringify(entry) });
        document.getElementById("entryForm").reset();
        showToast(res.message || "Entry added!", "ok");
        await loadDashboardData();
    } catch (err) {
        errBox.textContent = err.message;
        errBox.classList.remove("hidden");
        showToast(err.message, "err");
    }
}

async function deleteEntry(id) {
    try {
        const res = await callApi(`/timetable/${id}`, { method: "DELETE" });
        showToast(res.message || "Entry deleted", "ok");
        await loadDashboardData();
    } catch (err) {
        showToast(err.message, "err");
    }
}

// Run conflict detection in Java backend and display cards
async function detectConflicts(scope) {
    const banner = document.getElementById("conflictBanner");
    const warning = document.getElementById("conflictWarning");
    const list = document.getElementById("conflictList");

    try {
        const data = await callApi(`/conflicts/${scope}`);
        const conflicts = data.conflicts || [];
        banner.className = "alert mt-16";
        warning.classList.add("hidden");

        if (data.totalEntries === 0) {
            banner.classList.add("hidden");
            warning.classList.remove("hidden");
            warning.textContent = "Warning: Timetable is empty. Add entries before running conflict checks.";
            list.innerHTML = "";
            return;
        }

        if (conflicts.length === 0) {
            banner.classList.add("alert-green");
            banner.textContent = "No scheduling conflicts found. Your timetable is valid!";
            list.innerHTML = "";
            return;
        }

        banner.classList.add("alert-red");
        banner.textContent = `${conflicts.length} ${data.label} detected!`;

        if (conflicts.length >= 2) {
            warning.classList.remove("hidden");
            warning.textContent = "Warning: Multiple overlapping schedules detected. Check highlighted time slots.";
        }

        list.innerHTML = conflicts.map((c) => `
            <div class="conflict-card">
                <div class="conflict-top">
                    <span class="tag-red">${escapeHtml(c.conflictType)}</span>
                    <span class="tag-orange">Overlap: ${escapeHtml(c.timeOverlap)}</span>
                </div>
                <div><strong>Day:</strong> ${escapeHtml(c.day)} &nbsp;|&nbsp; <strong>Involved:</strong> ${escapeHtml(c.involved)}</div>
                <div class="pair-box">
                    <div class="entry-mini">
                        <strong>#${c.firstEntry.id} ${escapeHtml(c.firstEntry.subject)}</strong><br>
                        ${escapeHtml(c.firstEntry.faculty)} • ${escapeHtml(c.firstEntry.section)} • ${escapeHtml(c.firstEntry.room)}<br>
                        ${escapeHtml(c.firstEntry.startTime)} - ${escapeHtml(c.firstEntry.endTime)}
                    </div>
                    <div class="entry-mini">
                        <strong>#${c.secondEntry.id} ${escapeHtml(c.secondEntry.subject)}</strong><br>
                        ${escapeHtml(c.secondEntry.faculty)} • ${escapeHtml(c.secondEntry.section)} • ${escapeHtml(c.secondEntry.room)}<br>
                        ${escapeHtml(c.secondEntry.startTime)} - ${escapeHtml(c.secondEntry.endTime)}
                    </div>
                </div>
                <div class="alert alert-red">${escapeHtml(c.explanation)}</div>
            </div>
        `).join("");
    } catch (err) {
        showToast(err.message, "err");
    }
}

function renderWeeklyGrid(containerId) {
    const grid = document.getElementById(containerId);
    grid.innerHTML = DAYS.map((day) => {
        const items = allEntries
            .filter((e) => e.day.toLowerCase() === day.toLowerCase())
            .sort((a, b) => a.startTime.localeCompare(b.startTime));

        const slots = items.length
            ? items.map((e) => `
                <div class="slot">
                    <strong>${escapeHtml(e.startTime)} - ${escapeHtml(e.endTime)}</strong><br>
                    <strong>${escapeHtml(e.subject)}</strong><br>
                    ${escapeHtml(e.faculty)}<br>
                    ${escapeHtml(e.section)} • ${escapeHtml(e.room)}
                </div>`).join("")
            : `<p class="muted">No classes</p>`;

        return `<div class="day-col"><h4>${day}</h4>${slots}</div>`;
    }).join("");
}

function showToast(msg, type = "ok") {
    const box = document.getElementById("toastBox");
    const el = document.createElement("div");
    el.className = `toast ${type}`;
    el.textContent = msg;
    box.appendChild(el);
    setTimeout(() => el.remove(), 3000);
}

function escapeHtml(text) {
    return String(text ?? "")
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}
