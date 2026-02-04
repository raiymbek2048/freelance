import { useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Layout } from '@/components/layout';
import { Button, Card, Input, Textarea, Select } from '@/components/ui';
import { ordersApi } from '@/api/orders';
import { categoriesApi } from '@/api/categories';

const orderSchema = z.object({
  title: z.string().min(10, 'Минимум 10 символов').max(100, 'Максимум 100 символов'),
  description: z.string().min(50, 'Минимум 50 символов'),
  categoryId: z.number({ message: 'Выберите категорию' }).min(1, 'Выберите категорию'),
  budgetMin: z.number().optional(),
  budgetMax: z.number().optional(),
  deadline: z.string().optional(),
});

type OrderForm = z.infer<typeof orderSchema>;

export function CreateOrderPage() {
  const navigate = useNavigate();

  const { data: categories } = useQuery({
    queryKey: ['categories'],
    queryFn: categoriesApi.getAll,
  });

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
    watch,
    formState: { errors },
  } = useForm<OrderForm>({
    resolver: zodResolver(orderSchema),
    defaultValues: {
      categoryId: undefined,
    },
  });

  const selectedCategoryId = watch('categoryId');

  const categoryOptions = categories?.map((c) => ({ value: c.id, label: c.name })) || [];

  const onSubmit = (data: OrderForm) => {
    createMutation.mutate({
      ...data,
      attachments: [],
    });
  };

  return (
    <Layout>
      <div className="max-w-3xl mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-gray-900 mb-2">Создать заказ</h1>
        <p className="text-gray-600 mb-8">
          Опишите вашу задачу, и исполнители откликнутся на неё
        </p>

        <Card padding="lg">
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
            <Input
              label="Название заказа"
              placeholder="Например: Разработка мобильного приложения"
              error={errors.title?.message}
              {...register('title')}
            />

            <Select
              label="Категория"
              options={categoryOptions}
              placeholder="Выберите категорию"
              error={errors.categoryId?.message}
              value={selectedCategoryId || ''}
              onChange={(e) => {
                const value = e.target.value;
                setValue('categoryId', value ? Number(value) : undefined as any, { shouldValidate: true });
              }}
            />

            <Textarea
              label="Описание задачи"
              placeholder="Подробно опишите, что нужно сделать. Чем подробнее описание, тем точнее будут предложения исполнителей."
              rows={8}
              error={errors.description?.message}
              {...register('description')}
            />

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Input
                label="Бюджет от (сом)"
                type="number"
                placeholder="Минимальный бюджет"
                error={errors.budgetMin?.message}
                {...register('budgetMin', { valueAsNumber: true })}
              />
              <Input
                label="Бюджет до (сом)"
                type="number"
                placeholder="Максимальный бюджет"
                error={errors.budgetMax?.message}
                {...register('budgetMax', { valueAsNumber: true })}
              />
            </div>

            <Input
              label="Желаемый срок выполнения"
              type="date"
              error={errors.deadline?.message}
              {...register('deadline')}
            />

            <div className="pt-4 border-t border-gray-200 flex justify-end gap-3">
              <Button variant="outline" type="button" onClick={() => navigate(-1)}>
                Отмена
              </Button>
              <Button type="submit" loading={createMutation.isPending}>
                Опубликовать заказ
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </Layout>
  );
}
