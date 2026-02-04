import { Star } from 'lucide-react';
import { clsx } from 'clsx';

interface RatingProps {
  value: number;
  max?: number;
  size?: 'sm' | 'md' | 'lg';
  showValue?: boolean;
  onChange?: (value: number) => void;
}

export function Rating({ value, max = 5, size = 'md', showValue, onChange }: RatingProps) {
  const sizes = {
    sm: 'w-4 h-4',
    md: 'w-5 h-5',
    lg: 'w-6 h-6',
  };

  const textSizes = {
    sm: 'text-sm',
    md: 'text-base',
    lg: 'text-lg',
  };

  const handleClick = (rating: number) => {
    if (onChange) {
      onChange(rating);
    }
  };

  return (
    <div className="flex items-center gap-1">
      {Array.from({ length: max }, (_, i) => i + 1).map((rating) => (
        <button
          key={rating}
          type="button"
          disabled={!onChange}
          onClick={() => handleClick(rating)}
          className={clsx(
            'transition-colors',
            onChange && 'hover:scale-110 cursor-pointer',
            !onChange && 'cursor-default'
          )}
        >
          <Star
            className={clsx(
              sizes[size],
              rating <= value
                ? 'fill-yellow-400 text-yellow-400'
                : 'fill-gray-200 text-gray-200'
            )}
          />
        </button>
      ))}
      {showValue && (
        <span className={clsx('ml-1 font-medium text-gray-700', textSizes[size])}>
          {value.toFixed(1)}
        </span>
      )}
    </div>
  );
}
