/**
 * Utility for formatting grammatical tags into human-readable descriptions.
 * Converts abbreviations like "1sg.pres" into "1st person singular, present".
 */

interface GrammaticalParts {
  person?: string;
  number?: string;
  tense?: string;
  aspect?: string;
  mood?: string;
  other?: string;
}

/**
 * Formats abbreviated grammatical tags into readable English descriptions.
 *
 * @param tag - Abbreviated tag like "1sg.pres", "2pl.past.aor", "imperative"
 * @returns Human-readable description like "1st person singular, present"
 *
 * @example
 * formatGrammaticalTag("1sg.pres") // "1st person singular, present"
 * formatGrammaticalTag("2pl.past.imperf") // "2nd person plural, past imperfect"
 * formatGrammaticalTag("3sg.imperative") // "3rd person singular, imperative"
 */
export function formatGrammaticalTag(tag: string | null | undefined): string {
  if (!tag) return '';

  // Remove exclamation marks (used for imperatives like "пиши!")
  const cleanTag = tag.replace(/!/g, '').trim();

  if (!cleanTag) return '';

  // Split by dots to get individual parts
  const parts = cleanTag.split('.');

  const formatted: GrammaticalParts = {};

  for (const part of parts) {
    const lower = part.toLowerCase();

    // Person and number combinations
    if (lower.match(/^[123][sp][gl]$/)) {
      const person = lower[0];
      const number = lower.slice(1);

      // Person
      switch (person) {
        case '1':
          formatted.person = '1st person';
          break;
        case '2':
          formatted.person = '2nd person';
          break;
        case '3':
          formatted.person = '3rd person';
          break;
      }

      // Number
      if (number === 'sg') {
        formatted.number = 'singular';
      } else if (number === 'pl') {
        formatted.number = 'plural';
      }
    }
    // Tense
    else if (lower === 'pres' || lower === 'present') {
      formatted.tense = 'present';
    }
    else if (lower === 'past') {
      formatted.tense = 'past';
    }
    else if (lower === 'fut' || lower === 'future') {
      formatted.tense = 'future';
    }
    // Aspect
    else if (lower === 'aor' || lower === 'aorist') {
      formatted.aspect = 'aorist';
    }
    else if (lower === 'imperf' || lower === 'imperfect') {
      formatted.aspect = 'imperfect';
    }
    else if (lower === 'perf' || lower === 'perfect') {
      formatted.aspect = 'perfect';
    }
    // Mood
    else if (lower === 'imperative' || lower === 'imp') {
      formatted.mood = 'imperative';
    }
    else if (lower === 'conditional' || lower === 'cond') {
      formatted.mood = 'conditional';
    }
    // Other
    else {
      formatted.other = part;
    }
  }

  // Build the final string
  const result: string[] = [];

  if (formatted.person && formatted.number) {
    result.push(`${formatted.person} ${formatted.number}`);
  } else if (formatted.person) {
    result.push(formatted.person);
  } else if (formatted.number) {
    result.push(formatted.number);
  }

  if (formatted.tense) {
    result.push(formatted.tense);
  }

  if (formatted.aspect) {
    result.push(formatted.aspect);
  }

  if (formatted.mood) {
    result.push(formatted.mood);
  }

  if (formatted.other) {
    result.push(formatted.other);
  }

  return result.join(', ') || cleanTag;
}

/**
 * Formats grammatical tag with pronoun hint in parentheses.
 *
 * @param tag - Abbreviated tag like "1sg.pres"
 * @returns Description with pronoun like "1st person singular, present (I)"
 *
 * @example
 * formatWithPronoun("1sg.pres") // "1st person singular, present (I)"
 * formatWithPronoun("2pl.past.aor") // "2nd person plural, past aorist (you all)"
 */
export function formatWithPronoun(tag: string | null | undefined): string {
  if (!tag) return '';

  const formatted = formatGrammaticalTag(tag);
  const pronoun = getPronounHint(tag);

  if (pronoun) {
    return `${formatted} (${pronoun})`;
  }

  return formatted;
}

/**
 * Parses a noun grammatical tag into structured number and definiteness.
 * Handles tags like "sg.indef", "sg.def", "pl.indef", "pl.def".
 *
 * @param tag - Noun grammatical tag
 * @returns Parsed components, or null if the tag is not a recognisable noun tag
 *
 * @example
 * parseNounTag("sg.indef") // { number: 'singular', definiteness: 'indefinite' }
 * parseNounTag("pl.def")   // { number: 'plural', definiteness: 'definite' }
 */
export function parseNounTag(
  tag: string
): { number: 'singular' | 'plural'; definiteness: 'indefinite' | 'definite' } | null {
  if (!tag) return null;
  const parts = tag.toLowerCase().split('.');

  let number: 'singular' | 'plural' | null = null;
  for (const part of parts) {
    if (part === 'sg') number = 'singular';
    else if (part === 'pl') number = 'plural';
  }
  if (!number) return null;

  const isDefinite = parts.includes('def') && !parts.includes('indef');
  const definiteness: 'indefinite' | 'definite' = isDefinite ? 'definite' : 'indefinite';

  return { number, definiteness };
}

/**
 * Parses an adjective/pronoun grammatical tag into structured gender and definiteness.
 * Handles tags like "masc", "fem.def", "neut", "pl.def", "m", "f.def", etc.
 *
 * @param tag - Adjective grammatical tag
 * @returns Parsed components, or null if the tag is not a recognisable adjective tag
 *
 * @example
 * parseAdjectiveTag("masc")     // { gender: 'masculine', definiteness: 'indefinite' }
 * parseAdjectiveTag("fem.def")  // { gender: 'feminine', definiteness: 'definite' }
 * parseAdjectiveTag("pl")       // { gender: 'plural', definiteness: 'indefinite' }
 */
export function parseAdjectiveTag(
  tag: string
): { gender: 'masculine' | 'feminine' | 'neuter' | 'plural'; definiteness: 'indefinite' | 'definite' } | null {
  if (!tag) return null;
  const parts = tag.toLowerCase().split('.');

  let gender: 'masculine' | 'feminine' | 'neuter' | 'plural' | null = null;
  for (const part of parts) {
    if (part === 'masc' || part === 'm') gender = 'masculine';
    else if (part === 'fem' || part === 'f') gender = 'feminine';
    else if (part === 'neut' || part === 'n') gender = 'neuter';
    else if (part === 'pl') gender = 'plural';
  }
  if (!gender) return null;

  const isDefinite = parts.includes('def') && !parts.includes('indef');
  const definiteness: 'indefinite' | 'definite' = isDefinite ? 'definite' : 'indefinite';

  return { gender, definiteness };
}

/**
 * Gets a Bulgarian pronoun hint for a grammatical tag.
 *
 * @param tag - Abbreviated tag like "1sg", "2pl"
 * @returns Bulgarian pronoun like "аз", "ти", "ние", etc.
 */
function getPronounHint(tag: string): string {
  const lower = tag.toLowerCase();

  if (lower.includes('1sg')) return 'аз';
  if (lower.includes('2sg')) return 'ти';
  if (lower.includes('3sg')) return 'той/тя/то';
  if (lower.includes('1pl')) return 'ние';
  if (lower.includes('2pl')) return 'вие';
  if (lower.includes('3pl')) return 'те';

  return '';
}
