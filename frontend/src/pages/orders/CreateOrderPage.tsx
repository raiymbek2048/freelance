import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { MapPin, ChevronDown } from 'lucide-react';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Textarea } from '@/components/ui';
import { ordersApi } from '@/api/orders';

const cities = [
  'Бишкек',
  'Ош',
  'Джалал-Абад',
  'Каракол',
  'Токмок',
  'Нарын',
  'Талас',
  'Баткен',
];

const orderSchema = z.object({
  title: z.string().min(10, 'Минимум 10 символов').max(100, 'Максимум 100 символов'),
  description: z.string().min(20, 'Минимум 20 символов'),
  budget: z.number().optional(),
  deadlineDate: z.string().min(1, 'Укажите дату'),
  deadlineTime: z.string().min(1, 'Укажите время'),
  location: z.string().optional(),
});

type OrderForm = z.infer<typeof orderSchema>;

export function CreateOrderPage() {
  const navigate = useNavigate();
  const [locationEnabled, setLocationEnabled] = useState(true);
  const [selectedCity, setSelectedCity] = useState('Бишкек');

  const createMutation = useMutation({
    mutationFn: ordersApi.create,
    onSuccess: (order) => {
      navigate(`/orders/${order.id}`);
    },
  });

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<OrderForm>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      deadlineDate: new Date().toISOString().split('T')[0],
      deadlineTime: '12:00',
      location: 'Бишкек, Кыргызстан',
    },
  });

  const onSubmit = (data: OrderForm) => {
    const deadline = `${data.deadlineDate}T${data.deadlineTime}:00`;
    const location = locationEnabled ? `${selectedCity}, Кыргызстан` : 'Удаленно';
    createMutation.mutate({
      title: data.title,
      description: data.description,
      categoryId: 1,
      budgetMin: data.budget,
      budgetMax: data.budget,
      deadline,
      location,
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
                <MapPin className="w-4 h-4 inline mr-1" />
                Локация
              </label>
              <div className="space-y-3">
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    checked={!locationEnabled}
                    onChange={() => setLocationEnabled(false)}
                    className="w-4 h-4 text-cyan-500 focus:ring-cyan-500"
                  />
                  <span className="text-sm text-gray-600">Удалённо (любая локация)</span>
                </label>
                <label className="flex items-center gap-2 cursor-pointer">
                  <input
                    type="radio"
                    checked={locationEnabled}
                    onChange={() => setLocationEnabled(true)}
                    className="w-4 h-4 text-cyan-500 focus:ring-cyan-500"
                  />
                  <span className="text-sm text-gray-600">Указать город</span>
                </label>
                {locationEnabled && (
                  <div className="flex gap-3 ml-6">
                    <div className="relative">
                      <select
                        value={selectedCity}
                        onChange={(e) => setSelectedCity(e.target.value)}
                        className="appearance-none pl-3 pr-8 py-2 border border-gray-300 rounded-lg text-sm bg-white focus:outline-none focus:ring-2 focus:ring-cyan-500 cursor-pointer"
                      >
                        {cities.map((city) => (
                          <option key={city} value={city}>{city}</option>
                        ))}
                      </select>
                      <ChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none" />
                    </div>
                    <div className="flex items-center px-3 py-2 bg-gray-100 rounded-lg text-sm text-gray-600">
                      Кыргызстан
                    </div>
                  </div>
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
                Выполнить до
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
              </div>
              {(errors.deadlineDate || errors.deadlineTime) && (
                <p className="text-red-500 text-xs mt-1">Укажите дату и время</p>
              )}
            </div>

            {/* Budget */}
            <div>
              <Input
                label="Стоимость (сом)"
                type="number"
                placeholder="Укажите стоимость"
                error={errors.budget?.message}
                {...register('budget', { valueAsNumber: true })}
              />
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
