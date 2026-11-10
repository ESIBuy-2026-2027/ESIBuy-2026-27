import { environment } from '../../../environments/environment';

export function resolveImageUrl(url: string | null | undefined): string | null {
  if (!url) {
    return null;
  }
  if (
    url.startsWith('http://') ||
    url.startsWith('https://') ||
    url.startsWith('data:') ||
    url.startsWith('blob:')
  ) {
    return url;
  }
  const baseApi =
    (typeof window !== 'undefined' && window.localStorage?.getItem('esibuy_api_url')) ||
    environment.apiUrl;
  if (!baseApi) {
    return url;
  }
  const cleanBase = baseApi.endsWith('/') ? baseApi.slice(0, -1) : baseApi;
  const cleanUrl = url.startsWith('/') ? url : `/${url}`;
  return `${cleanBase}${cleanUrl}`;
}
