import { useState, useEffect } from 'react';
import Layout from '@/components/layout/Layout';
import { VocabularyList } from '@/features/vocabulary/components/VocabularyList';
import { VocabularyDetail } from '@/features/vocabulary/components/VocabularyDetail';
import { CreateVocabularyModal } from '@/features/vocabulary/components/CreateVocabularyModal';
import { EditVocabularyModal } from '@/features/vocabulary/components/EditVocabularyModal';
import { DeleteConfirmDialog } from '@/features/vocabulary/components/DeleteConfirmDialog';
import { useVocabularyUIStore } from '@/features/vocabulary/stores/useVocabularyUIStore';
import { useAuth } from '@/features/auth/useAuth';
import { LoginPage } from '@/features/auth/LoginPage';
import { useStudyStore } from '@/features/study/stores/useStudyStore';
import { useDueCount } from '@/features/study/api/useDueCount';
import { StudyLauncher } from '@/features/study/components/StudyLauncher';
import { FlashcardView } from '@/features/study/components/FlashcardView';
import { SessionSummaryView } from '@/features/study/components/SessionSummaryView';

type AppView = 'vocabulary' | 'study';

function App() {
  const { data: user, isLoading } = useAuth();
  const [appView, setAppView] = useState<AppView>('vocabulary');
  const [selectedLemmaId, setSelectedLemmaId] = useState<number | null>(null);
  const { phase: studyPhase } = useStudyStore();
  const { data: dueCount } = useDueCount();
  const totalDue = (dueCount?.dueToday ?? 0) + (dueCount?.newCards ?? 0);

  const deletingLemmaId = useVocabularyUIStore(state => state.deletingLemmaId);

  useEffect(() => {
    if (deletingLemmaId !== null && deletingLemmaId === selectedLemmaId) {
      setSelectedLemmaId(null);
    }
  }, [deletingLemmaId, selectedLemmaId]);

  if (isLoading) return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
    </div>
  );

  if (!user) return <LoginPage />;

  const handleViewDetail = (id: number) => setSelectedLemmaId(id);
  const handleBack = () => setSelectedLemmaId(null);

  return (
    <Layout>
      {/* Tab navigation */}
      <div className="border-b border-gray-200 mb-6 -mt-2">
        <nav className="flex gap-1">
          <button
            onClick={() => setAppView('vocabulary')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              appView === 'vocabulary'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Vocabulary
          </button>
          <button
            onClick={() => setAppView('study')}
            className={`relative px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              appView === 'study'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Study
            {totalDue > 0 && (
              <span className="absolute -top-0.5 -right-1 inline-flex items-center justify-center w-4 h-4
                               bg-orange-500 text-white text-xs rounded-full font-bold leading-none">
                {totalDue > 9 ? '9+' : totalDue}
              </span>
            )}
          </button>
        </nav>
      </div>

      {/* Vocabulary view */}
      {appView === 'vocabulary' && (
        <>
          {selectedLemmaId === null ? (
            <VocabularyList onViewDetail={handleViewDetail} />
          ) : (
            <VocabularyDetail lemmaId={selectedLemmaId} onBack={handleBack} />
          )}
        </>
      )}

      {/* Study view */}
      {appView === 'study' && (
        <>
          {studyPhase === 'idle' && <StudyLauncher />}
          {studyPhase === 'active' && <FlashcardView />}
          {studyPhase === 'summary' && <SessionSummaryView />}
        </>
      )}

      {/* CRUD Modals */}
      <CreateVocabularyModal />
      <EditVocabularyModal />
      <DeleteConfirmDialog />
    </Layout>
  );
}

export default App;
