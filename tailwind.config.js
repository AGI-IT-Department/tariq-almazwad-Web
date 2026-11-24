/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        parchment: {
          DEFAULT: '#FAF6EE',
          variant: '#F1E9DA',
          outline: '#DDD3C0',
          border: '#E8DFCE',
        },
        midnight: {
          DEFAULT: '#13313D',
          dark: '#0C212A',
          surface: '#0E1D24',
          card: '#162B34',
          elevated: '#1E3945',
        },
        gold: {
          warm: '#C89D4C',
          bright: '#E5BF70',
          container: '#FFF2D6',
        },
        burgundy: {
          DEFAULT: '#6E262C',
          light: '#D9878E',
          container: '#FBE5E7',
        },
        ink: {
          deep: '#13262F',
          muted: '#54666E',
        },
        status: {
          approved: '#1B7A4B',
          approvedBg: '#E2F6EB',
          pending: '#B56900',
          pendingBg: '#FFF2D8',
          rejected: '#B3261E',
          rejectedBg: '#FCE8E6',
        },
        medal: {
          gold: '#D4AF37',
          silver: '#8C9BA5',
          bronze: '#B87333',
        },
        whatsapp: '#1E8E49',
      },
      fontFamily: {
        sans: ['Cairo', 'system-ui', '-apple-system', 'sans-serif'],
        serif: ['Amiri', 'Georgia', 'serif'],
      },
    },
  },
  plugins: [],
};
