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
        className="rounded border border-border px-4 py-2 disabled:opacity-40"
      >
        Previous
      </button>
      <span className="text-muted">
        Page {page + 1} of {totalPages}
      </span>
      <button
        type="button"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
        className="rounded border border-border px-4 py-2 disabled:opacity-40"
      >
        Next
      </button>
    </div>
  );
}
