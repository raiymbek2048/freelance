import { useQuery } from '@tanstack/react-query';
import { Megaphone } from 'lucide-react';
import { subscriptionApi } from '@/api/subscription';

export function AnnouncementBanner() {
  const { data } = useQuery({
    queryKey: ['announcement'],
    queryFn: subscriptionApi.getAnnouncement,
    staleTime: 5 * 60 * 1000, // 5 minutes
  });

  if (!data?.enabled || !data?.message) {
    return null;
  }

  return (
    <div className="bg-amber-50 border-b border-amber-200 px-4 py-3">
      <div className="max-w-7xl mx-auto flex items-center justify-center gap-2">
        <Megaphone className="w-4 h-4 text-amber-600 flex-shrink-0" />
        <p className="text-amber-800 text-sm text-center">{data.message}</p>
      </div>
    </div>
  );
}
