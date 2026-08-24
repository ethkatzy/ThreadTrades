import Link from "next/link";

type RatingBadgeProps = {
  userId: number;
  averageRating: number | null;
  reviewCount: number;
  className?: string;
};

export function RatingBadge({ userId, averageRating, reviewCount, className }: RatingBadgeProps) {
  return (
    <Link
      href={`/users/${userId}/reviews`}
      className={`inline-flex items-center gap-1 text-sm underline ${className ?? ""}`}
    >
      {averageRating === null ? (
        <span className="text-muted">No ratings yet</span>
      ) : (
        <>
          <span className="text-amber-500 dark:text-amber-400" aria-hidden="true">
            ★
          </span>
          <span>
            {averageRating.toFixed(1)} ({reviewCount})
          </span>
        </>
      )}
    </Link>
  );
}
