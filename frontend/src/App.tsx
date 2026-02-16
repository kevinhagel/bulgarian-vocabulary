import { useState, useEffect } from 'react';
import Layout from '@/components/layout/Layout';
import { VocabularyList } from '@/features/vocabulary/components/VocabularyList';
import { VocabularyDetail } from '@/features/vocabulary/components/VocabularyDetail';
import { CreateVocabularyModal } from '@/features/vocabulary/components/CreateVocabularyModal';
import { EditVocabularyModal } from '@/features/vocabulary/components/EditVocabularyModal';
import { DeleteConfirmDialog } from '@/features/vocabulary/components/DeleteConfirmDialog';
import { useVocabularyUIStore } from '@/features/vocabulary/stores/useVocabularyUIStore';

function App() {
  // Simple state-based navigation
  const [selectedLemmaId, setSelectedLemmaId] = useState<number | null>(null);

  // Watch for delete operations on currently viewed lemma
  const deletingLemmaId = useVocabularyUIStore(state => state.deletingLemmaId);

  // Navigate back to list if the currently viewed lemma is deleted
  useEffect(() => {
    if (deletingLemmaId !== null && deletingLemmaId === selectedLemmaId) {
      setSelectedLemmaId(null);
    }
  }, [deletingLemmaId, selectedLemmaId]);

  // Handlers
  const handleViewDetail = (id: number) => {
    setSelectedLemmaId(id);
  };

  const handleBack = () => {
    setSelectedLemmaId(null);
  };

  return (
    <Layout>
      {/* Conditional rendering based on navigation state */}
      {selectedLemmaId === null ? (
        <VocabularyList onViewDetail={handleViewDetail} />
      ) : (
        <VocabularyDetail lemmaId={selectedLemmaId} onBack={handleBack} />
      )}

      {/* CRUD Modals - managed by Zustand store state */}
      <CreateVocabularyModal />
      <EditVocabularyModal />
      <DeleteConfirmDialog />
    </Layout>
  );
}

export default App;
