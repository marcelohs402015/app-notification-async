/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      colors: {
        brand: {
          50: "#eff6ff",
          100: "#dbeafe",
          500: "#3b82f6",
          600: "#2563eb",
          700: "#1d4ed8",
          900: "#1e3a8a",
        },
      },
      keyframes: {
        shake: {
          "0%, 100%": { transform: "rotate(0deg)" },
          "15%": { transform: "rotate(-15deg)" },
          "30%": { transform: "rotate(10deg)" },
          "45%": { transform: "rotate(-10deg)" },
          "60%": { transform: "rotate(6deg)" },
          "75%": { transform: "rotate(-4deg)" },
        },
      },
      animation: {
        shake: "shake 0.6s ease-in-out",
      },
    },
  },
  plugins: [],
};
