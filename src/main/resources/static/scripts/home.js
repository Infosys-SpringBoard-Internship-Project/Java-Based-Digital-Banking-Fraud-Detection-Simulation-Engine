document.addEventListener('DOMContentLoaded', () => {
  const counters = document.querySelectorAll('.stat-number');
  const duration = 2000;
  const fps = 60;
  const totalFrames = (duration / 1000) * fps;

  counters.forEach(counter => {
    const target = +counter.getAttribute('data-target');
    let frame = 0;

    const countUp = () => {
      frame++;
      const progress = frame / totalFrames;
      const value = Math.ceil(target * (1 - Math.pow(1 - progress, 3)));
      counter.innerText = value;

      if (frame < totalFrames) {
        requestAnimationFrame(countUp);
      } else {
        counter.innerText = target;
      }
    };

    requestAnimationFrame(countUp);
  });

  const token = localStorage.getItem('fraud_token');
  if (token) {
    document.getElementById('hero-login-btn')?.remove();
    document.getElementById('footer-login-link')?.remove();
  }

  syncSuperadminSetupVisibility();
});

async function syncSuperadminSetupVisibility() {
  try {
    const response = await fetch('/auth/bootstrap-status');
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
      return;
    }

    const shouldShowSetup = !!data.setupRequired;
    const heroSetup = document.getElementById('hero-setup-btn');
    const footerSetup = document.getElementById('footer-setup-link');
    if (heroSetup) heroSetup.style.display = shouldShowSetup ? 'inline-flex' : 'none';
    if (footerSetup) footerSetup.style.display = shouldShowSetup ? 'inline-flex' : 'none';
  } catch (error) {
    // Keep default visibility when bootstrap status is unavailable.
  }
}
