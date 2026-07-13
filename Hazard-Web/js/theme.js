const themeToggle = document.getElementById("themeToggle");

function applyTheme(theme) {
  const isDark = theme === "dark";

  document.body.classList.toggle("dark-mode", isDark);

  if (themeToggle) {
    themeToggle.textContent = isDark
      ? "☀️ Light Mode"
      : "🌙 Dark Mode";

    themeToggle.classList.toggle(
      "btn-outline-light",
      isDark
    );

    themeToggle.classList.toggle(
      "btn-outline-dark",
      !isDark
    );
  }
}

const savedTheme =
  localStorage.getItem("hazardTheme") || "light";

applyTheme(savedTheme);

if (themeToggle) {
  themeToggle.addEventListener("click", () => {
    const nextTheme =
      document.body.classList.contains("dark-mode")
        ? "light"
        : "dark";

    localStorage.setItem("hazardTheme", nextTheme);
    applyTheme(nextTheme);
  });
}