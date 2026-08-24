export type BadgeTone = "positive" | "warning" | "negative" | "neutral";

type StatusBadgeProps = {
  label: string;
  tone: BadgeTone;
  className?: string;
};

const TONE_CLASSES: Record<BadgeTone, string> = {
  positive: "bg-accent/15 text-accent",
  warning: "bg-amber-500/15 text-amber-700 dark:text-amber-400",
  negative: "bg-red-500/15 text-red-700 dark:text-red-400",
  neutral: "bg-muted/15 text-muted",
};

export function StatusBadge({ label, tone, className }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${TONE_CLASSES[tone]} ${className ?? ""}`}
    >
      {label}
    </span>
  );
}

export function swapStatusTone(status: "PENDING" | "ACCEPTED" | "REJECTED"): BadgeTone {
  if (status === "ACCEPTED") return "positive";
  if (status === "REJECTED") return "negative";
  return "warning";
}

export function itemStatusTone(status: "AVAILABLE" | "SWAPPED" | "REMOVED"): BadgeTone {
  if (status === "AVAILABLE") return "positive";
  if (status === "REMOVED") return "negative";
  return "neutral";
}

export function bannedTone(banned: boolean): BadgeTone {
  return banned ? "negative" : "positive";
}

export function roleTone(role: "USER" | "ADMIN" | null): BadgeTone {
  return role === "ADMIN" ? "positive" : "neutral";
}
