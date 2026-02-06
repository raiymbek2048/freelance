import type { ReactNode } from 'react';
import { Header } from './Header';
import { Footer } from './Footer';

interface LayoutProps {
  children: ReactNode;
  showFooter?: boolean;
  showHeader?: boolean;
  showBackground?: boolean;
}

export function Layout({ children, showFooter = true, showHeader = true, showBackground = true }: LayoutProps) {
  return (
    <div
      className="min-h-screen flex flex-col"
      style={showBackground ? {
        backgroundImage: 'url(/bishkek-bg.png)',
        backgroundSize: 'cover',
        backgroundPosition: 'center bottom',
        backgroundRepeat: 'no-repeat',
        backgroundAttachment: 'fixed',
      } : undefined}
    >
      {showHeader && <Header />}
      <main className="flex-1">{children}</main>
      {showFooter && <Footer />}
    </div>
  );
}
