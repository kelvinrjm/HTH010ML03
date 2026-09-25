import React, { createContext, useContext, useEffect, useState } from 'react';
import { api, clearAuthToken, getAuthToken, setAuthToken } from '../services/api';
import { User } from '../types';

interface AuthContextType {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
  isAdmin: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const handleUnauthorized = () => {
      setUser(null);
    };
    window.addEventListener('auth:unauthorized', handleUnauthorized);

    const token = getAuthToken();
    if (token) {
      api
        .getMe()
        .then((userData) => setUser(userData))
        .catch(() => clearAuthToken())
        .finally(() => setIsLoading(false));
    } else {
      setIsLoading(false);
    }

    return () => window.removeEventListener('auth:unauthorized', handleUnauthorized);
  }, []);

  const login = async (username: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await api.login({ username, password });
      setAuthToken(response.access_token);
      setUser(response.user);
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    clearAuthToken();
    setUser(null);
  };

  const isAdmin = user?.role === 'admin';

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        login,
        logout,
        isAdmin,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
