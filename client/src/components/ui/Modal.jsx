import React, { useEffect, useId, useRef } from 'react';
import { X } from 'lucide-react';

/**
 * Native <dialog> modal following modern-web-guidance:
 * - Opened with showModal() for built-in top layer, Esc, focus containment & inert backdrop.
 * - Light-dismiss fallback via pointerdown/pointerup outside bounding rect when closedBy is unsupported.
 */
export function Modal({ open, onClose, title, subtitle, maxWidth = 'max-w-lg', children }) {
  const dialogRef = useRef(null);
  const titleId = useId();

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    if (open && !dialog.open) {
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;

    const handleClose = () => {
      dialog.returnValue = '';
      if (open) onClose?.();
    };

    const isOutside = (e) => {
      const r = dialog.getBoundingClientRect();
      return e.clientX < r.left || e.clientX > r.right || e.clientY < r.top || e.clientY > r.bottom;
    };

    let pressedOutside = false;
    const onPointerDown = (e) => {
      pressedOutside = isOutside(e);
    };
    const onPointerUp = (e) => {
      if (pressedOutside && isOutside(e)) {
        dialog.close();
      }
      pressedOutside = false;
    };

    dialog.addEventListener('close', handleClose);
    if (!('closedBy' in HTMLDialogElement.prototype)) {
      dialog.addEventListener('pointerdown', onPointerDown);
      dialog.addEventListener('pointerup', onPointerUp);
    }

    return () => {
      dialog.removeEventListener('close', handleClose);
      dialog.removeEventListener('pointerdown', onPointerDown);
      dialog.removeEventListener('pointerup', onPointerUp);
    };
  }, [open, onClose]);

  return (
    <dialog
      ref={dialogRef}
      closedby="any"
      aria-labelledby={titleId}
      className={`m-auto w-[calc(100%-2rem)] ${maxWidth} rounded-2xl p-0 shadow-2xl`}
    >
      <div className="flex items-start justify-between gap-4 border-b border-[var(--border-subtle)] px-6 py-4">
        <div>
          <h2 id={titleId} className="text-base font-semibold text-[var(--text-primary)]">
            {title}
          </h2>
          {subtitle && (
            <p className="mt-0.5 text-xs text-[var(--text-secondary)]">{subtitle}</p>
          )}
        </div>
        <button
          type="button"
          onClick={() => dialogRef.current?.close()}
          aria-label="Fechar janela"
          className="inline-flex h-8 w-8 items-center justify-center rounded-lg text-[var(--text-muted)] hover:bg-[var(--surface-hover)] hover:text-[var(--text-primary)] transition-colors cursor-pointer"
        >
          <X size={16} />
        </button>
      </div>
      <div className="px-6 py-5 max-h-[80vh] overflow-y-auto">{children}</div>
    </dialog>
  );
}

export default Modal;
