import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Textarea } from '@/components/ui';
import { ordersApi } from '@/api/orders';

const orderSchema = z.object({
  title: z.string().min(10, 'Минимум 10 символов').max(100, 'Максимум 100 символов'),
  description: z.string().min(20, 'Минимум 20 символов'),
  budgetMin: z.number().optional(),
  budgetMax: z.number().optional(),
  deadlineDate: z.string().min(1, 'Укажите дату'),
  deadlineTime: z.string().min(1, 'Укажите время'),
  location: z.string().optional(),
});

type OrderForm = z.infer<typeof orderSchema>;

export function CreateOrderPage() {
  const navigate = useNavigate();
  const [location, setLocation] = useState('Бишкек, Кыргызстан');

  const createMutation = useMutation({
    mutationFn: ordersApi.create,
    onSuccess: (order) => {
      navigate(`/orders/${order.id}`);
    },
  });

  const {
    register,
    handleSubmit,
    setValue,
    formState: { errors },
  } = useForm<OrderForm>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      deadlineDate: new Date().toISOString().split('T')[0],
      deadlineTime: '12:00',
      location: 'Бишкек, Кыргызстан',
    },
  });

  // Quick time buttons
  const setQuickDeadline = (hours: number) => {
    const now = new Date();
    now.setHours(now.getHours() + hours);
    setValue('deadlineDate', now.toISOString().split('T')[0]);
    setValue('deadlineTime', now.toTimeString().slice(0, 5));
  };

  const onSubmit = (data: OrderForm) => {
    const deadline = `${data.deadlineDate}T${data.deadlineTime}:00`;
    createMutation.mutate({
      title: data.title,
      description: data.description,
      categoryId: 1,
      budgetMin: data.budgetMin,
      budgetMax: data.budgetMax,
      deadline,
      attachments: [],
    });
  };

  return (
    <Layout>
      <div className="max-w-2xl mx-auto px-4 py-8">
        <h1 className="text-2xl font-bold text-gray-900 mb-6">Создание задания</h1>

        <Card padding="lg">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            {/* Location */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Локация
              </label>
              <div className="flex items-center gap-4">
                <label className="flex items-center gap-2">
                  <input
                    type="radio"
                    checked={!location}
                    onChange={() => setLocation('')}
                    className="text-cyan-500"
                  />
                  <span className="text-sm text-gray-600">Задание может выполнить исполнитель из любой локации</span>
                </label>
              </div>
              <div className="mt-2 flex items-center gap-4">
                <label className="flex items-center gap-2">
                  <input
                    type="radio"
                    checked={!!location}
                    onChange={() => setLocation('Бишкек, Кыргызстан')}
                    className="text-cyan-500"
                  />
                  <span className="text-sm text-gray-600">Город</span>
                </label>
                {location && (
                  <input
                    type="text"
                    value={location}
                    onChange={(e) => setLocation(e.target.value)}
                    className="px-3 py-1.5 border border-gray-300 rounded-lg text-sm"
                    placeholder="Бишкек, Кыргызстан"
                  />
                )}
              </div>
            </div>

            {/* Description (they call it description but it's actually the main task) */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Описание
              </label>
              <Textarea
                placeholder="Опишите задание подробно..."
                rows={6}
                error={errors.description?.message}
                {...register('description')}
              />
              <p className="text-xs text-gray-400 mt-1">
                Подробно опишите, что нужно сделать
              </p>
            </div>

            {/* Title */}
            <Input
              label="Название задания"
              placeholder="Краткое название задания"
              error={errors.title?.message}
              {...register('title')}
            />

            {/* Deadline */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Срок выполнения
              </label>
              <div className="flex items-center gap-3 flex-wrap">
                <input
                  type="date"
                  {...register('deadlineDate')}
                  className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
                />
                <input
                  type="time"
                  {...register('deadlineTime')}
                  className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
                />
                <button
                  type="button"
                  onClick={() => setQuickDeadline(2)}
                  className="px-3 py-2 border border-cyan-500 text-cyan-500 rounded-lg text-sm hover:bg-cyan-50"
                >
                  Через 2 часа
                </button>
                <button
                  type="button"
                  onClick={() => setQuickDeadline(6)}
                  className="px-3 py-2 border border-cyan-500 text-cyan-500 rounded-lg text-sm hover:bg-cyan-50"
                >
                  Через 6 часов
                </button>
              </div>
              {(errors.deadlineDate || errors.deadlineTime) && (
                <p className="text-red-500 text-xs mt-1">Укажите дату и время</p>
              )}
              <p className="text-xs text-gray-400 mt-2">
                Укажите дату и время, к которым вам нужен результат задания.
              </p>
            </div>

            {/* Budget */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Стоимость (сом)
              </label>
              <div className="grid grid-cols-2 gap-4">
                <Input
                  type="number"
                  placeholder="От"
                  error={errors.budgetMin?.message}
                  {...register('budgetMin', { valueAsNumber: true })}
                />
                <Input
                  type="number"
                  placeholder="До"
                  error={errors.budgetMax?.message}
                  {...register('budgetMax', { valueAsNumber: true })}
                />
              </div>
            </div>

            {/* Submit */}
            <div className="pt-4 flex justify-end gap-3">
              <Button variant="outline" type="button" onClick={() => navigate(-1)}>
                Отмена
              </Button>
              <Button type="submit" loading={createMutation.isPending}>
                Опубликовать задание
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
