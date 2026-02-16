interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
}

/**
 * Pagination controls for navigating through pages of vocabulary entries.
 */
export function Pagination({ currentPage, totalPages, onPageChange }: PaginationProps) {
  if (totalPages <= 1) {
    return null; // No pagination needed for single page
  }

  // Generate page numbers with ellipsis for large sets
  const pageNumbers: (number | 'ellipsis')[] = [];
  const maxVisible = 7; // Max number of page buttons to show

  if (totalPages <= maxVisible) {
    // Show all pages if total is less than max
    for (let i = 0; i < totalPages; i++) {
      pageNumbers.push(i);
    }
  } else {
    // Always show first page
    pageNumbers.push(0);

    // Determine range around current page
    let start = Math.max(1, currentPage - 1);
    let end = Math.min(totalPages - 2, currentPage + 1);

    // Adjust range if near start or end
    if (currentPage <= 2) {
      end = 3;
    }
    if (currentPage >= totalPages - 3) {
      start = totalPages - 4;
    }

    // Add ellipsis before middle section if needed
    if (start > 1) {
      pageNumbers.push('ellipsis');
    }

    // Add middle section
    for (let i = start; i <= end; i++) {
      pageNumbers.push(i);
    }

    // Add ellipsis after middle section if needed
    if (end < totalPages - 2) {
      pageNumbers.push('ellipsis');
    }

    // Always show last page
    pageNumbers.push(totalPages - 1);
  }

  return (
    <div className="flex items-center justify-center gap-1 mt-6">
      {/* Previous button */}
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage === 0}
        className="px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        aria-label="Previous page"
      >
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="15 18 9 12 15 6" />
        </svg>
      </button>

      {/* Page numbers */}
      {pageNumbers.map((page, index) =>
        page === 'ellipsis' ? (
          <span key={`ellipsis-${index}`} className="px-3 py-2 text-gray-500">
            ...
          </span>
        ) : (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={`px-3 py-2 text-sm font-medium rounded-md transition-colors ${
              currentPage === page
                ? 'text-white bg-blue-600 hover:bg-blue-700'
                : 'text-gray-700 bg-white border border-gray-300 hover:bg-gray-50'
            }`}
            aria-label={`Page ${page + 1}`}
            aria-current={currentPage === page ? 'page' : undefined}
          >
            {page + 1}
          </button>
        )
      )}

      {/* Next button */}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage === totalPages - 1}
        className="px-3 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
        aria-label="Next page"
      >
        <svg className="w-4 h-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <polyline points="9 18 15 12 9 6" />
        </svg>
      </button>
    </div>
  );
}
