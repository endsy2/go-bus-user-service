# Performance Optimization for /api/users/profile

## Problem
The `/api/users/profile` endpoint was taking ~9 seconds to respond due to:
1. **Synchronous MinIO calls** - Every profile request made a blocking call to MinIO to generate presigned URLs
2. **N+1 query problem** - User, roles, and permissions were loaded in separate queries
3. **No caching** - Presigned URLs (valid for 7 days) were regenerated on every request

## Solutions Implemented

### 1. Async MinIO with Timeout
**File**: `src/main/java/com/busapp/userservice/util/MinioUtil.java`
- Added `getPresignedUrlWithTimeout()` method with 200ms timeout
- Uses `CompletableFuture` with `completeOnTimeout()` for non-blocking calls
- Falls back to `null` if MinIO takes longer than 200ms
- **Impact**: Guarantees fast response even on first request

### 2. Async Thread Pool Configuration
**File**: `src/main/java/com/busapp/userservice/config/AsyncConfig.java`
- Created dedicated thread pool for MinIO operations
- Core pool: 5 threads, Max pool: 10 threads
- Prevents blocking main request threads
- **Impact**: Parallel MinIO calls without blocking API responses

### 3. Redis Caching for Presigned URLs
**File**: `src/main/java/com/busapp/userservice/util/MinioUtil.java`
- Added `@Cacheable` annotation to `getPresignedUrl()` method
- Cache name: `presignedUrls`
- TTL: 6 days (URLs valid for 7 days, refresh before expiry)
- **Impact**: Eliminates MinIO API calls for cached URLs

### 4. Cache Manager Configuration
**File**: `src/main/java/com/busapp/userservice/config/RedisConfig.java`
- Added `@EnableCaching` annotation
- Configured `CacheManager` bean with custom TTL per cache
- presignedUrls cache: 6 days TTL
- Default cache: 1 hour TTL

### 5. Optimized Database Query
**File**: `src/main/java/com/busapp/userservice/repository/UserRepository.java`
- Added `findByIdWithRolesAndPermissions()` method
- Uses `JOIN FETCH` to load user, roles, and permissions in a single query
- **Impact**: Reduces database round trips from N+1 to 1 query

### 6. Updated Service Layer
**File**: `src/main/java/com/busapp/userservice/service/impl/UserServiceImpl.java`
- Updated `getUserById()` to use the optimized query method
- **Impact**: Faster data retrieval with fewer database queries

### 7. Updated Mapper with Timeout
**File**: `src/main/java/com/busapp/userservice/dto/mapper/UserMapper.java`
- Changed to use `getPresignedUrlWithTimeout(image, 200)` instead of blocking call
- **Impact**: Ensures response within 300-400ms even on first request

## Expected Performance Improvement

### First Request (Cold Start)
- **Before**: ~9 seconds
- **After**: ~300-400ms
- MinIO call runs with 200ms timeout
- If MinIO responds within 200ms: presigned URL returned
- If MinIO takes longer: returns null, but response is still fast
- **Improvement**: ~95% reduction

### Subsequent Requests (Cached)
- **Before**: ~9 seconds
- **After**: ~100-200ms
- Presigned URL served from Redis cache
- No MinIO call needed
- **Improvement**: ~98% reduction

## Behavior

1. **First request**: 
   - Attempts to get presigned URL from MinIO with 200ms timeout
   - If successful: returns presigned URL
   - If timeout: returns null for profilePicture, but response is still fast
   - URL gets cached for 6 days

2. **Subsequent requests**:
   - Presigned URL served from Redis cache instantly
   - No MinIO call needed
   - Consistent fast response

3. **Cache expiry**:
   - After 6 days, cache expires
   - Next request regenerates and caches the URL
   - Still maintains 300-400ms response time with timeout

## Testing
1. First call to `/api/users/profile` should respond in ~300-400ms
2. Check if `profilePicture` field has a value (MinIO responded in time) or null (timeout)
3. Subsequent calls should be ~100-200ms with cached presigned URL
4. Monitor Redis cache hit rate
5. Check application logs for "Generated presigned URL" (cache miss) vs no log (cache hit)

## Additional Recommendations
1. Consider adding database indexes on frequently queried columns if not present
2. Monitor Redis memory usage as cache grows
3. Monitor async thread pool metrics
4. Add application metrics to track endpoint response times
5. Consider implementing cache warming for frequently accessed users
