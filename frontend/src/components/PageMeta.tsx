import { Helmet } from 'react-helmet-async';
import { useLocation } from 'react-router-dom';

interface PageMetaProps {
  title: string;
  description?: string;
}

export function PageMeta({ title, description }: PageMetaProps) {
  const { pathname } = useLocation();
  const canonicalUrl = `https://freelance.kg${pathname}`;

  return (
    <Helmet>
      <title>{title}</title>
      {description && <meta name="description" content={description} />}
      <meta property="og:title" content={`${title} | Фриланс КГ`} />
      {description && <meta property="og:description" content={description} />}
      <meta property="og:url" content={canonicalUrl} />
      <link rel="canonical" href={canonicalUrl} />
    </Helmet>
  );
}
