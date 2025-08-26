import { createTheme } from '@mui/material/styles';

const createAppTheme = (mode = 'light') => {
  const isDark = mode === 'dark';
  
  return createTheme({
    palette: {
      mode,
      primary: {
        main: isDark ? '#90caf9' : '#2563EB',
        light: isDark ? '#e3f2fd' : '#3b82f6',
        dark: isDark ? '#42a5f5' : '#1d4ed8',
      },
      secondary: {
        main: isDark ? '#81c784' : '#10B981',
        light: isDark ? '#c8e6c9' : '#34d399',
        dark: isDark ? '#4caf50' : '#059669',
      },
      warning: {
        main: '#F59E0B',
      },
      error: {
        main: '#EF4444',
      },
      background: {
        default: isDark ? '#0a0a0a' : '#f5f5f5',
        paper: isDark ? '#1a1a1a' : '#ffffff',
      },
      text: {
        primary: isDark ? '#ffffff' : '#000000',
        secondary: isDark ? '#b3b3b3' : '#666666',
      },
    },
    typography: {
      fontFamily: 'Pretendard, Arial, sans-serif',
      h1: {
        fontSize: '2rem',
        fontWeight: 600,
      },
      h2: {
        fontSize: '1.5rem',
        fontWeight: 600,
      },
      h3: {
        fontSize: '1.25rem',
        fontWeight: 500,
      },
      body1: {
        fontSize: '1rem',
        lineHeight: 1.5,
      },
    },
    breakpoints: {
      values: {
        xs: 0,
        sm: 600,
        md: 900,
        lg: 1200,
        xl: 1536,
      },
    },
    components: {
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            scrollbarWidth: 'thin',
            '&::-webkit-scrollbar': {
              width: '8px',
            },
            '&::-webkit-scrollbar-track': {
              background: isDark ? '#2a2a2a' : '#f1f1f1',
            },
            '&::-webkit-scrollbar-thumb': {
              background: isDark ? '#555' : '#c1c1c1',
              borderRadius: '4px',
            },
            '&::-webkit-scrollbar-thumb:hover': {
              background: isDark ? '#777' : '#a1a1a1',
            },
          },
        },
      },
      MuiAppBar: {
        styleOverrides: {
          root: {
            backgroundColor: isDark ? '#1a1a1a' : '#2563EB',
            color: isDark ? '#ffffff' : '#ffffff',
          },
        },
      },
      MuiDrawer: {
        styleOverrides: {
          paper: {
            backgroundColor: isDark ? '#1a1a1a' : '#ffffff',
            borderRight: isDark ? '1px solid #333' : '1px solid #e0e0e0',
          },
        },
      },
      MuiPaper: {
        styleOverrides: {
          root: {
            backgroundImage: isDark ? 'none' : undefined,
            backgroundColor: isDark ? '#1a1a1a' : '#ffffff',
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            boxShadow: isDark
              ? '0 2px 12px rgba(0,0,0,0.5)'
              : '0 2px 12px rgba(0,0,0,0.08)',
            backgroundColor: isDark ? '#1a1a1a' : '#ffffff',
          },
        },
      },
      MuiButton: {
        styleOverrides: {
          root: {
            textTransform: 'none',
            borderRadius: 8,
          },
        },
      },
    },
  });
};

export default createAppTheme;