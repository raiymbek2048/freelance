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
    <div className="min-h-screen flex flex-col relative">
      {showBackground && (
        <>
          {/* Blurred background layer */}
          <div
            className="fixed inset-0 -z-10"
            style={{
              backgroundImage: 'url(/bishkek-bg.png)',
              backgroundSize: 'cover',
              backgroundPosition: 'center bottom',
              backgroundRepeat: 'no-repeat',
              filter: 'blur(8px)',
              transform: 'scale(1.1)',
            }}
          />
          {/* Dark overlay */}
          <div className="fixed inset-0 -z-10 bg-black/50" />
        </>
      )}
      {showHeader && <Header />}
      <main className="flex-1">{children}</main>
      {showFooter && <Footer />}
    </div>
  );
}
