import { environment } from '../../../environments/environment';
import { resolveImageUrl } from './image.util';

describe('resolveImageUrl', () => {
  const originalApiUrl = environment.apiUrl;

  afterEach(() => {
    environment.apiUrl = originalApiUrl;
    localStorage.removeItem('esibuy_api_url');
  });

  it('should return null when url is null, undefined or empty', () => {
    expect(resolveImageUrl(null)).toBeNull();
    expect(resolveImageUrl(undefined)).toBeNull();
    expect(resolveImageUrl('')).toBeNull();
  });

  it('should return absolute http and https URLs unchanged', () => {
    expect(resolveImageUrl('https://example.com/photo.png')).toBe('https://example.com/photo.png');
    expect(resolveImageUrl('http://example.com/photo.png')).toBe('http://example.com/photo.png');
  });

  it('should return data and blob URLs unchanged', () => {
    expect(resolveImageUrl('data:image/png;base64,abc')).toBe('data:image/png;base64,abc');
    expect(resolveImageUrl('blob:http://localhost/123')).toBe('blob:http://localhost/123');
  });

  it('should return relative url as-is when baseApi is empty', () => {
    environment.apiUrl = '';
    expect(resolveImageUrl('/api/products/1/image')).toBe('/api/products/1/image');
  });

  it('should prepend environment apiUrl when baseApi is configured', () => {
    environment.apiUrl = 'https://backend.example.com';
    expect(resolveImageUrl('/api/products/1/image')).toBe('https://backend.example.com/api/products/1/image');
    expect(resolveImageUrl('api/products/1/image')).toBe('https://backend.example.com/api/products/1/image');
  });

  it('should handle trailing slash in environment apiUrl', () => {
    environment.apiUrl = 'https://backend.example.com/';
    expect(resolveImageUrl('/api/products/1/image')).toBe('https://backend.example.com/api/products/1/image');
  });

  it('should prioritize localStorage esibuy_api_url if present', () => {
    environment.apiUrl = 'https://default.example.com';
    localStorage.setItem('esibuy_api_url', 'https://custom.example.com');
    expect(resolveImageUrl('/api/products/1/image')).toBe('https://custom.example.com/api/products/1/image');
  });
});
