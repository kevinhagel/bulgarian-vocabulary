export default function Header() {
  return (
    <header className="sticky top-0 z-10 bg-white shadow">
      <div className="container mx-auto px-4 py-4">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between">
          <h1 className="text-2xl font-bold text-gray-900">
            Bulgarian Vocabulary
          </h1>
          <nav className="mt-2 md:mt-0">
            {/* Navigation items will be added in later plans */}
          </nav>
        </div>
      </div>
    </header>
  );
}
