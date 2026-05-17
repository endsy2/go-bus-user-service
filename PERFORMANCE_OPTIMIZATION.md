# Performance Optimization for /api/users/profile

## Problem
The `/api/users/profile` endpoint was taking ~9 seconds to respond due to:
1. **Synchronous MinIO calls** - Every profile request made a blocking call to MinIO to generate presigned URLs
2. **N+1 query problem** - User, roles, and permissions were loaded in separate queries
3. **No caching** - Presigned URLs (valid for 7 days) were regenerated on every request

## Solutions Implemented

### 1. Redis Caching for Presigned URLs
**File**: `src/main/java/com/busapp/userservice/util/MinioUtil.java`
- Added `@Cacheable` annotation to `getPresignedUrl()` method
- Cache name: `presignedUrls`
- TTL: 6 days (URLs valid for 7 days, refresh before expiry)
- **Impact**: Eliminates MinIO API calls for cached URLs

### 2. Cache Manager Configuration
**File**: `src/main/java/com/busapp/userservice/config/RedisConfig.java`
- Added `@EnableCaching` annotation
- Configured `CacheManager` bean with custom TTL per cache
- presignedUrls cache: 6 days TTL
- Default cache: 1 hour TTL

### 3. Optimized Database Query
**File**: `src/main/java/com/busapp/userservice/repository/UserRepository.java`
- Added `findByIdWithRolesAndPermissions()` method
- Uses `JOIN FETCH` to load user, roles, and permissions in a single query
- **Impact**: Reduces database round trips from N+1 to 1 query

### 4. Updated Service Layer
**File**: `src/main/java/com/busapp/userservice/service/impl/UserServiceImpl.java`
- Updated `getUserById()` to use the optimized query method
- **Impact**: Faster data retrieval with fewer database queries

## Expected Performance Improvement
- **First request**: ~2-3 seconds (one-time MinIO call + optimized query)
- **Subsequent requests**: ~100-300ms (cached presigned URL + single query)
- **Overall improvement**: ~90-95% reduction in response time

## Testing
1. First call to `/api/users/profile` will populate the cache
2. Subsequent calls should be significantly faster
3. Monitor Redis cache hit rate
4. Check application logs for "Generated presigned URL" (cache miss) vs no log (cache hit)

## Additional Recommendations
1. Consider adding database indexes on frequently queried columns if not present
2. Monitor Redis memory usage as cache grows
3. Consider implementing cache warming for frequently accessed users
4. Add application metrics to track endpoint response times
