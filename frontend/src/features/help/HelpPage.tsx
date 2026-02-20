import { useState } from 'react';

interface SectionProps {
  title: string;
  children: React.ReactNode;
}

function Section({ title, children }: SectionProps) {
  const [open, setOpen] = useState(false);
  return (
    <div className="border border-gray-200 rounded-xl overflow-hidden">
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center justify-between px-5 py-4 bg-white hover:bg-gray-50
                   transition-colors text-left"
      >
        <span className="font-semibold text-gray-900">{title}</span>
        <svg
          className={`w-5 h-5 text-gray-400 transition-transform ${open ? 'rotate-180' : ''}`}
          viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"
        >
          <path d="M6 9l6 6 6-6" />
        </svg>
      </button>
      {open && (
        <div className="px-5 py-4 bg-gray-50 border-t border-gray-200 text-sm text-gray-700 space-y-2">
          {children}
        </div>
      )}
    </div>
  );
}

/**
 * Help / FAQ page. Static content — no API calls.
 */
export function HelpPage() {
  return (
    <div className="max-w-2xl mx-auto px-4 py-8">
      <h1 className="text-2xl font-bold text-gray-900 mb-2">Help & Guide</h1>
      <p className="text-gray-500 mb-8 text-sm">How to use the Bulgarian Vocabulary Tutor</p>

      <div className="space-y-3">
        <Section title="What is this app?">
          <p>
            A personal Bulgarian vocabulary tutor. You add Bulgarian words and phrases; the app
            analyses them using a local AI model (BgGPT), generates full inflection tables, stores
            example sentences, and schedules spaced-repetition flashcard reviews.
          </p>
          <p>
            Everything runs locally — no internet required for AI features, no subscription.
          </p>
        </Section>

        <Section title="Adding vocabulary">
          <p>
            Click <strong>+ Add Word</strong> in the Vocabulary tab. Enter any word form —
            inflected (e.g. <span lang="bg">чете</span>) or lemma (<span lang="bg">чета</span>).
            The AI detects the canonical form automatically.
          </p>
          <p>
            Processing takes 30–90 seconds. The word appears immediately with status
            <em> Pending</em>, and inflections appear once processing finishes.
          </p>
          <p>
            <strong>Translation</strong> is optional — the app auto-translates via Google Translate
            during processing. Provide your own to override.
          </p>
          <p>
            <strong>Notes</strong> help the AI disambiguate: for <span lang="bg">минал</span> (adj,
            "past/gone") vs the verb <span lang="bg">мина</span>, add a note like
            "adjective, NOT the verb мина".
          </p>
        </Section>

        <Section title="Bulk add (Word Lists)">
          <p>
            Inside a Word List, tap <strong>Add Words</strong> to enter many words at once —
            one per line:
          </p>
          <pre className="bg-white border border-gray-200 rounded p-3 mt-1 text-xs font-mono">
{`чета
пера (notes: verb - to wash)
казвам се
обичам`}
          </pre>
          <p className="mt-2">
            Use <code className="bg-white px-1 rounded border border-gray-200 text-xs">
              (notes: …)</code> after a word to pass disambiguation hints.
          </p>
          <p>
            Duplicate words are handled gracefully — the app finds the existing entry and
            adds it to the list.
          </p>
        </Section>

        <Section title="Review queue">
          <p>
            New words start with review status <strong>Pending</strong>. Words in
            Pending or Needs Correction state will <em>not</em> appear in flashcard study.
          </p>
          <p>
            Open the <strong>Review</strong> tab to see all words waiting for approval.
            Check inflections, then click <strong>Mark Reviewed</strong> to add the word
            to the study pool. Use <strong>Flag for Correction</strong> if something looks wrong,
            then use <strong>Reprocess</strong> to re-run the AI.
          </p>
        </Section>

        <Section title="Study / Flashcards">
          <p>
            Only <strong>Reviewed</strong> words appear in study. The app uses the
            SM-2 spaced-repetition algorithm — the same one used by Anki.
          </p>
          <ul className="list-disc ml-4 space-y-1">
            <li><strong>Correct</strong> — you knew it. Interval increases.</li>
            <li><strong>Incorrect</strong> — you didn't. Card resets to tomorrow.</li>
            <li><strong>Skip</strong> — skip without affecting the schedule.</li>
          </ul>
          <p>
            The Study tab badge shows how many cards are due today
            (orange = due + new; yellow Review badge = words awaiting approval).
          </p>
        </Section>

        <Section title="Example sentences">
          <p>
            Open any word's detail page and scroll to <strong>Example Sentences</strong>.
            Click <strong>Generate Sentences</strong> to ask Qwen 2.5 14B to write
            4 natural Bulgarian sentences using that word. This takes 60–90 seconds.
          </p>
          <p>
            Each sentence has an audio play button so you can hear the pronunciation.
            Click <strong>Regenerate</strong> to get a fresh set of sentences.
          </p>
          <p>
            In the Vocabulary list, use the <strong>Generate All Sentences</strong> button
            to backfill sentences for all words that don't have them yet (queued in batches of 50).
          </p>
        </Section>

        <Section title="Word Lists">
          <p>
            The <strong>Lists</strong> tab lets you organise vocabulary by topic, lesson,
            or any grouping. You can study directly from a list — only words in that list
            are shown during that session.
          </p>
        </Section>

        <Section title="Audio playback">
          <p>
            The play button (▶) on any word or sentence generates audio using Microsoft
            Edge TTS (neural Bulgarian voice). Audio is cached so repeated plays are instant.
          </p>
        </Section>

        <Section title="Accented forms">
          <p>
            In the inflections table, some forms show a stress mark over the stressed vowel
            (e.g. <span lang="bg">часа́</span>). This uses Unicode combining acute accent (U+0301).
            Stress marks are especially useful where the same spelling has two different
            meanings with different stress (e.g. <span lang="bg">замъ́к</span> castle vs
            <span lang="bg">зама́к</span> lock).
          </p>
        </Section>

        <Section title="Reprocess / Disambiguation">
          <p>
            If the AI chose the wrong lemma or generated wrong inflections, click
            <strong> Reprocess</strong> on the detail page. You can add a disambiguation hint —
            a short note telling the AI what you mean.
          </p>
          <p>Examples of good hints:</p>
          <ul className="list-disc ml-4 space-y-1">
            <li>"adjective, NOT the verb мина"</li>
            <li>"noun meaning verb (grammar term), NOT the verb глаголя"</li>
            <li>"reflexive verb, means 'to be called'"</li>
          </ul>
        </Section>
      </div>
    </div>
  );
}
