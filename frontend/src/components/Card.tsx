import type { ElementType, ReactNode } from "react";

type CardProps = {
  as?: ElementType;
  className?: string;
  children: ReactNode;
};

export function Card({ as: Component = "div", className, children }: CardProps) {
  return (
    <Component className={`rounded-lg border border-border bg-surface p-3 ${className ?? ""}`}>
      {children}
    </Component>
  );
}
