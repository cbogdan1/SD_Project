import { HttpErrorResponse } from '@angular/common/http';

// Backend errors use ExceptionHandlerResponseDTO from the common module: { message, details[], ... }
export function errorText(err: HttpErrorResponse): string {
  const details = Array.isArray(err.error?.details) ? err.error.details.join(', ') : '';
  return [err.error?.message ?? err.statusText, details].filter(Boolean).join(': ');
}
