    document.addEventListener("DOMContentLoaded", () => {
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
          // Ease-out logic
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
    });
