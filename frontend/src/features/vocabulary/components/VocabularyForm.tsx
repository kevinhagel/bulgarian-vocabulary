import { useForm, useFieldArray } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { createVocabularySchema, updateVocabularySchema } from '../schemas/vocabularySchemas';
import type { CreateVocabularyFormData, UpdateVocabularyFormData } from '../schemas/vocabularySchemas';

interface VocabularyFormProps {
  mode: 'create' | 'edit';
  defaultValues?: Partial<CreateVocabularyFormData | UpdateVocabularyFormData>;
  onSubmit: (data: any) => Promise<void>;
  isSubmitting: boolean;
  onCancel: () => void;
}

/**
 * Reusable form component for creating and editing vocabulary entries.
 * Uses React Hook Form with Zod validation.
 * Create mode: accepts wordForm (any inflected form).
 * Edit mode: works with canonical lemma text and supports inflection editing.
 */
export function VocabularyForm({ mode, defaultValues, onSubmit, isSubmitting, onCancel }: VocabularyFormProps) {
  const schema = mode === 'create' ? createVocabularySchema : updateVocabularySchema;

  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(schema),
    defaultValues: defaultValues as any,
  });

  // Field array for inflections (edit mode only)
  const { fields, append, remove } = useFieldArray({
    control,
    name: 'inflections' as any,
  });

  const handleFormSubmit = async (data: any) => {
    await onSubmit(data);
  };

  return (
    <form onSubmit={handleSubmit(handleFormSubmit)} className="space-y-4">
      {/* Word form (create) or Lemma text (edit) */}
      <div>
        <label htmlFor={mode === 'create' ? 'wordForm' : 'text'} className="block text-sm font-medium text-gray-700 mb-1">
          {mode === 'create' ? 'Bulgarian Word' : 'Lemma Text'}
        </label>
        <input
          id={mode === 'create' ? 'wordForm' : 'text'}
          type="text"
          lang="bg"
          placeholder={mode === 'create' ? 'Enter Bulgarian word or phrase' : 'Canonical lemma form'}
          className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            errors.wordForm || errors.text ? 'border-red-500' : 'border-gray-300'
          }`}
          {...register(mode === 'create' ? 'wordForm' : 'text')}
        />
        {(errors.wordForm || errors.text) && (
          <p className="mt-1 text-sm text-red-600">
            {(errors.wordForm?.message || errors.text?.message) as string}
          </p>
        )}
      </div>

      {/* Translation */}
      <div>
        <label htmlFor="translation" className="block text-sm font-medium text-gray-700 mb-1">
          English Translation {mode === 'create' && <span className="text-gray-500 font-normal">(optional - auto-translated if empty)</span>}
        </label>
        <input
          id="translation"
          type="text"
          placeholder={mode === 'create' ? 'Optional - will auto-translate' : 'Enter English translation'}
          className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            errors.translation ? 'border-red-500' : 'border-gray-300'
          }`}
          {...register('translation')}
        />
        {errors.translation && (
          <p className="mt-1 text-sm text-red-600">{errors.translation.message as string}</p>
        )}
      </div>

      {/* Notes */}
      <div>
        <label htmlFor="notes" className="block text-sm font-medium text-gray-700 mb-1">
          Notes
        </label>
        <textarea
          id="notes"
          rows={3}
          placeholder="Optional notes..."
          className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 ${
            errors.notes ? 'border-red-500' : 'border-gray-300'
          }`}
          {...register('notes')}
        />
        {errors.notes && (
          <p className="mt-1 text-sm text-red-600">{errors.notes.message as string}</p>
        )}
      </div>

      {/* Inflections (edit mode only) */}
      {mode === 'edit' && (
        <div>
          <div className="flex items-center justify-between mb-2">
            <label className="block text-sm font-medium text-gray-700">Inflections</label>
            <button
              type="button"
              onClick={() => append({ form: '', grammaticalInfo: '' })}
              className="text-sm text-blue-600 hover:text-blue-700 font-medium"
            >
              + Add Inflection
            </button>
          </div>
          <div className="space-y-2">
            {fields.map((field, index) => (
              <div key={field.id} className="flex gap-2 items-start">
                <div className="flex-1">
                  <input
                    type="text"
                    lang="bg"
                    placeholder="Form"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    {...register(`inflections.${index}.form` as any)}
                  />
                </div>
                <div className="flex-1">
                  <input
                    type="text"
                    placeholder="Grammatical info (optional)"
                    className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    {...register(`inflections.${index}.grammaticalInfo` as any)}
                  />
                </div>
                <button
                  type="button"
                  onClick={() => remove(index)}
                  className="text-red-600 hover:text-red-700 p-2"
                  aria-label="Remove inflection"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                  </svg>
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Form actions */}
      <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 text-gray-700 border border-gray-300 rounded-md hover:bg-gray-50 transition-colors"
          disabled={isSubmitting}
        >
          Cancel
        </button>
        <button
          type="submit"
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors disabled:bg-blue-300 disabled:cursor-not-allowed"
          disabled={isSubmitting}
        >
          {isSubmitting ? 'Saving...' : mode === 'create' ? 'Create Vocabulary' : 'Save Changes'}
        </button>
      </div>
    </form>
  );
}
