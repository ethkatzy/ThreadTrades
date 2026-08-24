type PaginationProps = {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
};

export function Pagination({ page, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null;
  }

  return (
    <div className="flex items-center justify-between gap-3 text-sm">
      <button
        type="button"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
        className="rounded border border-black/10 px-4 py-2 disabled:opacity-40 dark:border-white/20"
      >
        Previous
      </button>
      <span className="text-zinc-600 dark:text-zinc-400">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="rounded border border-black/10 px-4 py-2 disabled:opacity-40 dark:border-white/20"
      >
        Next
      </button>
    </div>
  );
}
