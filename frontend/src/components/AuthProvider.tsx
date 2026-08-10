"use client";

import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from "react";
import {
  getCurrentUser,
  login as apiLogin,
  register as apiRegister,
  type AuthResponse,
  type LoginInput,
  type RegisterInput,
  type UserProfile,
} from "@/lib/api/auth";
import { clearStoredToken, getStoredToken, setStoredToken } from "@/lib/api/token";

type AuthContextValue = {
  token: string | null;
  user: UserProfile | null;
  isLoading: boolean;
  login: (input: LoginInput) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    // localStorage can't be read during SSR/first render (no hydration
    // mismatch that way), so this has to resolve client-side in an effect.
    const storedToken = getStoredToken();
    if (!storedToken) {
      // eslint-disable-next-line react-hooks/set-state-in-effect -- see comment above
      setIsLoading(false);
      return;
    }
    getCurrentUser(storedToken)
      .then((profile) => {
        setToken(storedToken);
        setUser(profile);
      })
      .catch(() => {
        clearStoredToken();
      })
      .finally(() => setIsLoading(false));
  }, []);

  const applySession = useCallback(async (session: AuthResponse) => {
    setStoredToken(session.token);
    setToken(session.token);
    setUser(await getCurrentUser(session.token));
  }, []);

  const login = useCallback(
    async (input: LoginInput) => {
      await applySession(await apiLogin(input));
    },
    [applySession],
  );

  const register = useCallback(
    async (input: RegisterInput) => {
      await applySession(await apiRegister(input));
    },
    [applySession],
  );

  const logout = useCallback(() => {
    clearStoredToken();
    setToken(null);
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ token, user, isLoading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}
