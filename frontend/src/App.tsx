import { useState, useEffect } from 'react';
import Layout from '@/components/layout/Layout';
import { VocabularyList } from '@/features/vocabulary/components/VocabularyList';
import { VocabularyDetail } from '@/features/vocabulary/components/VocabularyDetail';
import { CreateVocabularyModal } from '@/features/vocabulary/components/CreateVocabularyModal';
import { EditVocabularyModal } from '@/features/vocabulary/components/EditVocabularyModal';
import { DeleteConfirmDialog } from '@/features/vocabulary/components/DeleteConfirmDialog';
import { ReviewQueueView } from '@/features/vocabulary/components/ReviewQueueView';
import { useVocabularyUIStore } from '@/features/vocabulary/stores/useVocabularyUIStore';
import { useAuth } from '@/features/auth/useAuth';
import { LoginPage } from '@/features/auth/LoginPage';
import { useStudyStore } from '@/features/study/stores/useStudyStore';
import { useDueCount } from '@/features/study/api/useDueCount';
import { StudyLauncher } from '@/features/study/components/StudyLauncher';
import { FlashcardView } from '@/features/study/components/FlashcardView';
import { SessionSummaryView } from '@/features/study/components/SessionSummaryView';
import { ProgressDashboard } from '@/features/study/components/ProgressDashboard';
import { useListsUIStore } from '@/features/lists/stores/useListsUIStore';
import { ListsView } from '@/features/lists/components/ListsView';
import { ListDetail } from '@/features/lists/components/ListDetail';
import { AddVocabularyToList } from '@/features/lists/components/AddVocabularyToList';
import { HelpPage } from '@/features/help/HelpPage';
import { AdminDashboard } from '@/features/admin/AdminDashboard';

type AppView = 'vocabulary' | 'study' | 'lists' | 'review' | 'help' | 'admin';

function App() {
  const { data: user, isLoading } = useAuth();
  const [appView, setAppView] = useState<AppView>('vocabulary');
  const [selectedLemmaId, setSelectedLemmaId] = useState<number | null>(null);
  const [addVocabListId, setAddVocabListId] = useState<number | null>(null);
  const { phase: studyPhase } = useStudyStore();
  const { data: dueCount } = useDueCount();
  const totalDue = (dueCount?.dueToday ?? 0) + (dueCount?.newCards ?? 0);
  const pendingReview = dueCount?.pendingReview ?? 0;

  const deletingLemmaId = useVocabularyUIStore(state => state.deletingLemmaId);
  const { selectedListId, clearSelection } = useListsUIStore();

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

  const handleSwitchTab = (tab: AppView) => {
    setAppView(tab);
    // Reset list selection when leaving lists tab
    if (tab !== 'lists') clearSelection();
  };

  return (
    <Layout>
      {/* Tab navigation */}
      <div className="border-b border-gray-200 mb-6 -mt-2">
        <nav className="flex gap-1">
          <button
            onClick={() => handleSwitchTab('vocabulary')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              appView === 'vocabulary'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Vocabulary
          </button>
          <button
            onClick={() => handleSwitchTab('study')}
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
          <button
            onClick={() => handleSwitchTab('lists')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              appView === 'lists'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Lists
          </button>
          <button
            onClick={() => handleSwitchTab('review')}
            className={`relative px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              appView === 'review'
                ? 'border-yellow-500 text-yellow-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Review
            {pendingReview > 0 && (
              <span className="absolute -top-0.5 -right-1 inline-flex items-center justify-center w-4 h-4
                               bg-yellow-500 text-white text-xs rounded-full font-bold leading-none">
                {pendingReview > 9 ? '9+' : pendingReview}
              </span>
            )}
          </button>
          <button
            onClick={() => handleSwitchTab('help')}
            className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
              appView === 'help'
                ? 'border-blue-600 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700'
            }`}
          >
            Help
          </button>
          {user?.isAdmin && (
            <button
              onClick={() => handleSwitchTab('admin')}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 transition-colors ${
                appView === 'admin'
                  ? 'border-purple-600 text-purple-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700'
              }`}
            >
              Admin
            </button>
          )}
        </nav>
      </div>

      {/* Vocabulary view */}
      {appView === 'vocabulary' && (
        <>
          {selectedLemmaId === null ? (
            <VocabularyList onViewDetail={handleViewDetail} onNavigateStudy={() => handleSwitchTab('study')} />
          ) : (
            <VocabularyDetail lemmaId={selectedLemmaId} onBack={handleBack} />
          )}
        </>
      )}

      {/* Study view */}
      {appView === 'study' && (
        <>
          {studyPhase === 'idle' && <><StudyLauncher onNavigateToReview={() => handleSwitchTab('review')} /><ProgressDashboard /></>}
          {studyPhase === 'active' && <FlashcardView />}
          {studyPhase === 'summary' && <SessionSummaryView />}
        </>
      )}

      {/* Lists view */}
      {appView === 'lists' && (
        <>
          {selectedListId === null ? (
            <ListsView />
          ) : (
            <ListDetail
              listId={selectedListId}
              onBack={clearSelection}
              onStudyStarted={() => setAppView('study')}
              onAddVocabulary={() => setAddVocabListId(selectedListId)}
            />
          )}
        </>
      )}

      {/* Review queue */}
      {appView === 'review' && (
        <ReviewQueueView
          onViewDetail={id => {
            setSelectedLemmaId(id);
            handleSwitchTab('vocabulary');
          }}
        />
      )}

      {/* Help page */}
      {appView === 'help' && <HelpPage />}

      {/* Admin dashboard */}
      {appView === 'admin' && user?.isAdmin && <AdminDashboard />}

      {/* CRUD Modals */}
      <CreateVocabularyModal />
      <EditVocabularyModal />
      <DeleteConfirmDialog />

      {/* Add vocabulary to list modal */}
      {addVocabListId !== null && (
        <AddVocabularyToList listId={addVocabListId} onClose={() => setAddVocabListId(null)} />
      )}
    </Layout>
  );
}

export default App;
