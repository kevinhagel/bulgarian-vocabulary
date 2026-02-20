import { useState } from 'react';
import { AudioPlayButton } from '@/components/audio/AudioPlayButton';
import { formatWithPronoun, parseNounTag, parseAdjectiveTag } from '@/utils/grammarFormatter';
import type { InflectionDTO, PartOfSpeech } from '@/types';

interface InflectionsTableProps {
  inflections: InflectionDTO[];
  partOfSpeech: PartOfSpeech | null;
}

// ============================================================
// Shared helpers
// ============================================================

/**
 * Renders the display form of an inflection (accentedForm if present, else form)
 * with a plain-form hint and audio button.
 */
function FormDisplay({ inf }: { inf: InflectionDTO }) {
  const display = inf.accentedForm ?? inf.form;
  return (
    <div className="flex items-center gap-2 flex-wrap">
      <span lang="bg" className="text-2xl font-semibold text-gray-900">
        {display}
      </span>
      {inf.accentedForm && (
        <span className="text-xs text-gray-400">({inf.form})</span>
      )}
      <AudioPlayButton text={display} />
    </div>
  );
}

function EmptyCell() {
  return <span className="text-gray-300 italic text-sm">—</span>;
}

// ============================================================
// NOUN GRID (singular/plural × indefinite/definite)
// ============================================================

function NounGrid({ inflections }: { inflections: InflectionDTO[] }) {
  const find = (number: 'singular' | 'plural', definiteness: 'indefinite' | 'definite') =>
    inflections.find((inf) => {
      const parsed = parseNounTag(inf.grammaticalInfo ?? '');
      return parsed?.number === number && parsed?.definiteness === definiteness;
    });

  const sgIndef = find('singular', 'indefinite');
  const sgDef = find('singular', 'definite');
  const plIndef = find('plural', 'indefinite');
  const plDef = find('plural', 'definite');

  if (!sgIndef && !sgDef && !plIndef && !plDef) {
    return <FlatList inflections={inflections} showDifficultyFilter={false} />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className="w-28 px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200" />
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200">
              Indefinite
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200">
              Definite
            </th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <th className="px-4 py-3 text-sm font-medium text-gray-600 bg-gray-50 border border-gray-200 text-left">
              Singular
            </th>
            <td className="px-4 py-4 border border-gray-200 bg-white">
              {sgIndef ? <FormDisplay inf={sgIndef} /> : <EmptyCell />}
            </td>
            <td className="px-4 py-4 border border-gray-200 bg-white">
              {sgDef ? <FormDisplay inf={sgDef} /> : <EmptyCell />}
            </td>
          </tr>
          <tr>
            <th className="px-4 py-3 text-sm font-medium text-gray-600 bg-gray-50 border border-gray-200 text-left">
              Plural
            </th>
            <td className="px-4 py-4 border border-gray-200 bg-white">
              {plIndef ? <FormDisplay inf={plIndef} /> : <EmptyCell />}
            </td>
            <td className="px-4 py-4 border border-gray-200 bg-white">
              {plDef ? <FormDisplay inf={plDef} /> : <EmptyCell />}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  );
}

// ============================================================
// VERB CONJUGATION TABLE (person × singular/plural, grouped by tense)
// ============================================================

type VerbTense = 'present' | 'past.aor' | 'past.imperf';

function getVerbTense(tag: string): VerbTense | 'imperative' | null {
  const parts = tag.toLowerCase().split('.');
  if (parts.includes('imperative') || parts.includes('imp')) return 'imperative';
  const hasPast = parts.includes('past');
  if (hasPast && (parts.includes('aor') || parts.includes('aorist'))) return 'past.aor';
  if (hasPast && (parts.includes('imperf') || parts.includes('imperfect'))) return 'past.imperf';
  if (parts.includes('pres') || parts.includes('present')) return 'present';
  return null;
}

function getPersonNumber(tag: string): string | null {
  const parts = tag.toLowerCase().split('.');
  for (const part of parts) {
    if (/^[123](sg|pl)$/.test(part)) return part;
  }
  return null;
}

const PERSON_ROWS: Array<{
  person: '1' | '2' | '3';
  label: string;
  pronounSg: string;
  pronounPl: string;
}> = [
  { person: '1', label: '1st', pronounSg: 'аз', pronounPl: 'ние' },
  { person: '2', label: '2nd', pronounSg: 'ти', pronounPl: 'вие' },
  { person: '3', label: '3rd', pronounSg: 'той/тя/то', pronounPl: 'те' },
];

const TENSE_LABELS: Record<VerbTense, string> = {
  present: 'Present Tense',
  'past.aor': 'Past Aorist',
  'past.imperf': 'Past Imperfect',
};

const TENSE_ORDER: VerbTense[] = ['present', 'past.aor', 'past.imperf'];

function TenseTable({
  tenseLabel,
  byPersonNumber,
}: {
  tenseLabel: string;
  byPersonNumber: Map<string, InflectionDTO>;
}) {
  return (
    <div className="mb-6">
      <h4 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-2">
        {tenseLabel}
      </h4>
      <div className="overflow-x-auto">
        <table className="w-full border-collapse">
          <thead>
            <tr>
              <th className="w-40 px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200" />
              <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200">
                Singular
              </th>
              <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200">
                Plural
              </th>
            </tr>
          </thead>
          <tbody>
            {PERSON_ROWS.map(({ person, label, pronounSg, pronounPl }) => {
              const sg = byPersonNumber.get(`${person}sg`);
              const pl = byPersonNumber.get(`${person}pl`);
              return (
                <tr key={person}>
                  <th className="px-4 py-3 text-sm bg-gray-50 border border-gray-200 text-left font-normal">
                    <span className="font-medium">{label}</span>
                    <div className="text-xs text-gray-500 mt-0.5">
                      <span lang="bg">{pronounSg}</span>
                      {' / '}
                      <span lang="bg">{pronounPl}</span>
                    </div>
                  </th>
                  <td className="px-4 py-3 border border-gray-200 bg-white">
                    {sg ? <FormDisplay inf={sg} /> : <EmptyCell />}
                  </td>
                  <td className="px-4 py-3 border border-gray-200 bg-white">
                    {pl ? <FormDisplay inf={pl} /> : <EmptyCell />}
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function VerbConjugationTable({ inflections }: { inflections: InflectionDTO[] }) {
  const [showBasicOnly, setShowBasicOnly] = useState(true);

  // Group inflections by tense
  const byTense = new Map<VerbTense, Map<string, InflectionDTO>>();
  const imperatives: InflectionDTO[] = [];

  for (const inf of inflections) {
    const tag = inf.grammaticalInfo ?? '';
    const tense = getVerbTense(tag);
    if (tense === 'imperative') {
      imperatives.push(inf);
      continue;
    }
    if (!tense) continue;
    if (!byTense.has(tense)) byTense.set(tense, new Map());
    const personNum = getPersonNumber(tag);
    if (personNum) byTense.get(tense)!.set(personNum, inf);
  }

  if (byTense.size === 0 && imperatives.length === 0) {
    return <FlatList inflections={inflections} showDifficultyFilter={true} />;
  }

  const tensesToShow = showBasicOnly
    ? (['present'] as VerbTense[]).filter((t) => byTense.has(t))
    : TENSE_ORDER.filter((t) => byTense.has(t));

  return (
    <div>
      {/* Filter toggle */}
      <div className="mb-4 flex items-center justify-between">
        <div className="text-sm text-gray-600">
          {showBasicOnly ? (
            <span>
              Showing <strong>present tense</strong>
            </span>
          ) : (
            <span>
              Showing <strong>all tenses</strong>
            </span>
          )}
        </div>
        <button
          onClick={() => setShowBasicOnly(!showBasicOnly)}
          className="px-4 py-2 text-sm font-medium text-blue-600 bg-blue-50 border border-blue-200 rounded-md hover:bg-blue-100 transition-colors"
        >
          {showBasicOnly ? 'Show all tenses' : 'Show present only'}
        </button>
      </div>

      {tensesToShow.map((tense) => (
        <TenseTable
          key={tense}
          tenseLabel={TENSE_LABELS[tense]}
          byPersonNumber={byTense.get(tense)!}
        />
      ))}

      {/* Imperative forms — shown only when all tenses visible */}
      {imperatives.length > 0 && !showBasicOnly && (
        <div className="mt-2">
          <h4 className="text-sm font-semibold text-gray-700 uppercase tracking-wider mb-2">
            Imperative
          </h4>
          <div className="flex flex-wrap gap-4">
            {imperatives.map((inf) => (
              <div key={inf.id} className="bg-white border border-gray-200 rounded-lg px-4 py-3">
                <FormDisplay inf={inf} />
                {inf.grammaticalInfo && (
                  <div className="text-xs text-gray-500 mt-1">{inf.grammaticalInfo}</div>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ============================================================
// ADJECTIVE / PRONOUN GRID (gender × indefinite/definite)
// ============================================================

const GENDER_ROWS = [
  { key: 'masculine' as const, label: 'Masculine' },
  { key: 'feminine' as const, label: 'Feminine' },
  { key: 'neuter' as const, label: 'Neuter' },
  { key: 'plural' as const, label: 'Plural' },
];

function AdjectiveGrid({ inflections }: { inflections: InflectionDTO[] }) {
  const find = (
    gender: 'masculine' | 'feminine' | 'neuter' | 'plural',
    definiteness: 'indefinite' | 'definite'
  ) =>
    inflections.find((inf) => {
      const parsed = parseAdjectiveTag(inf.grammaticalInfo ?? '');
      return parsed?.gender === gender && parsed?.definiteness === definiteness;
    });

  const hasGridData = inflections.some((inf) => !!parseAdjectiveTag(inf.grammaticalInfo ?? ''));
  if (!hasGridData) {
    return <FlatList inflections={inflections} showDifficultyFilter={false} />;
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr>
            <th className="w-28 px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200" />
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200">
              Indefinite
            </th>
            <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider bg-gray-50 border border-gray-200">
              Definite
            </th>
          </tr>
        </thead>
        <tbody>
          {GENDER_ROWS.map(({ key, label }) => {
            const indef = find(key, 'indefinite');
            const def = find(key, 'definite');
            if (!indef && !def) return null;
            return (
              <tr key={key}>
                <th className="px-4 py-3 text-sm font-medium text-gray-600 bg-gray-50 border border-gray-200 text-left">
                  {label}
                </th>
                <td className="px-4 py-4 border border-gray-200 bg-white">
                  {indef ? <FormDisplay inf={indef} /> : <EmptyCell />}
                </td>
                <td className="px-4 py-4 border border-gray-200 bg-white">
                  {def ? <FormDisplay inf={def} /> : <EmptyCell />}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

// ============================================================
// FLAT LIST (fallback for unrecognised POS or unrecognised tags)
// ============================================================

function FlatList({
  inflections,
  showDifficultyFilter,
}: {
  inflections: InflectionDTO[];
  showDifficultyFilter: boolean;
}) {
  const [showBasicOnly, setShowBasicOnly] = useState(true);

  const filtered =
    showDifficultyFilter && showBasicOnly
      ? inflections.filter((inf) => inf.difficultyLevel === 'BASIC' || !inf.difficultyLevel)
      : inflections;

  if (filtered.length === 0) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
        <p className="text-gray-700 font-medium">No inflections to show</p>
      </div>
    );
  }

  return (
    <div>
      {showDifficultyFilter && (
        <div className="mb-4 flex items-center justify-between">
          <div className="text-sm text-gray-600">
            {showBasicOnly ? (
              <span>
                Showing <strong>basic forms</strong> (аз, той/тя/то)
              </span>
            ) : (
              <span>
                Showing <strong>all forms</strong>
              </span>
            )}
          </div>
          <button
            onClick={() => setShowBasicOnly(!showBasicOnly)}
            className="px-4 py-2 text-sm font-medium text-blue-600 bg-blue-50 border border-blue-200 rounded-md hover:bg-blue-100 transition-colors"
          >
            {showBasicOnly ? 'Show all forms' : 'Show basic only'}
          </button>
        </div>
      )}

      <div className="overflow-x-auto">
        {/* Desktop */}
        <table className="hidden md:table min-w-full border-collapse bg-white rounded-lg overflow-hidden">
          <thead>
            <tr className="bg-gray-100 border-b border-gray-200">
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                Inflected Form
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                Grammatical Info
              </th>
              <th className="px-6 py-3 text-left text-xs font-medium text-gray-700 uppercase tracking-wider">
                Audio
              </th>
            </tr>
          </thead>
          <tbody>
            {filtered.map((inf, index) => (
              <tr
                key={inf.id}
                className={`border-b border-gray-200 ${index % 2 === 0 ? 'bg-white' : 'bg-gray-50'}`}
              >
                <td className="px-6 py-4">
                  <span lang="bg" className="text-2xl font-semibold text-gray-900">
                    {inf.accentedForm ?? inf.form}
                  </span>
                  {inf.accentedForm && (
                    <span className="text-xs text-gray-400 ml-1">({inf.form})</span>
                  )}
                </td>
                <td className="px-6 py-4 text-sm text-gray-600">
                  {formatWithPronoun(inf.grammaticalInfo) || '—'}
                </td>
                <td className="px-6 py-4">
                  <AudioPlayButton text={inf.accentedForm ?? inf.form} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Mobile */}
        <div className="md:hidden space-y-3">
          {filtered.map((inf) => (
            <div key={inf.id} className="bg-white border border-gray-200 rounded-lg p-4 shadow-sm">
              <div className="mb-3">
                <span lang="bg" className="text-2xl font-semibold text-gray-900">
                  {inf.accentedForm ?? inf.form}
                </span>
                {inf.accentedForm && (
                  <span className="text-xs text-gray-400 ml-1">({inf.form})</span>
                )}
              </div>
              <div className="mb-3 text-sm text-gray-600">
                <span className="font-medium text-gray-700">Grammatical Info:</span>{' '}
                {formatWithPronoun(inf.grammaticalInfo) || '—'}
              </div>
              <div>
                <AudioPlayButton text={inf.accentedForm ?? inf.form} />
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ============================================================
// MAIN COMPONENT — dispatches to the appropriate renderer
// ============================================================

/**
 * Responsive inflections display with structured grid rendering by part of speech.
 *
 * - NOUN       → 2×2 table (singular/plural × indefinite/definite)
 * - VERB       → conjugation tables by tense with person×number grid
 * - ADJECTIVE / PRONOUN → gender×definiteness grid
 * - Everything else → flat list with difficulty filter
 *
 * Falls back to flat list if tags cannot be parsed into a grid.
 */
export function InflectionsTable({ inflections, partOfSpeech }: InflectionsTableProps) {
  if (inflections.length === 0) {
    return (
      <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 text-center">
        <svg
          className="inline-block w-12 h-12 text-gray-400 mb-3"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          strokeWidth="2"
        >
          <circle cx="12" cy="12" r="10" />
          <line x1="12" y1="8" x2="12" y2="12" />
          <line x1="12" y1="16" x2="12.01" y2="16" />
        </svg>
        <p className="text-gray-700 font-medium">No inflections available</p>
        <p className="text-gray-600 text-sm mt-1">This lemma has no stored inflections.</p>
      </div>
    );
  }

  if (partOfSpeech === 'NOUN') return <NounGrid inflections={inflections} />;
  if (partOfSpeech === 'VERB') return <VerbConjugationTable inflections={inflections} />;
  if (partOfSpeech === 'ADJECTIVE' || partOfSpeech === 'PRONOUN')
    return <AdjectiveGrid inflections={inflections} />;

  return <FlatList inflections={inflections} showDifficultyFilter={true} />;
}
