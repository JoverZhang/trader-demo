---@param namespace string
---@param price number
---@param amount number
---@param isAscending string|boolean 匹配 `price` 的顺序, 'true' 为 从小到大, `false` 为 从大到小
local function match_orders(namespace, price, amount, isAscending)
    local priceQueueName = namespace .. '::prices'
    local orderQueueNamePrefix = namespace .. '::orders::'
    local orderMapName = namespace .. '::map'
    local resultSet = {}

    -- 交易是否已完成
    local isCompleted = false

    -- 根据指定的 `price` 顺序, 从 `price queue` 中获取符合 `price` 区间内的所有 `price`
    local prices;
    if isAscending == 'true' then
        prices = redis.call('ZRANGEBYSCORE', priceQueueName, 0, price)
    else
        prices = redis.call('ZREVRANGEBYSCORE', priceQueueName, '+INF', price)
    end
    for _, orderPrice in ipairs(prices) do
        -- 弹出 `order queue` 中的第一个 `order`
        local orderQueueName = orderQueueNamePrefix .. orderPrice
        local orderId = redis.call('LPOP', orderQueueName)
        while orderId ~= false do
            local orderAmount = redis.call('HGET', orderMapName, orderId)
            redis.call('HDEL', orderMapName, orderId)

            local diff = redis.call('DECIMAL', 'SUB', orderAmount, amount)

            -- orderAmount >= amount, 交易完成
            if tonumber(diff) >= 0 then
                -- 挂单仍有剩余量
                if tonumber(diff) > 0 then
                    redis.call('HSET', orderMapName, orderId, diff)
                    redis.call('LPUSH', orderQueueName, orderId)
                end
                table.insert(resultSet, orderId .. ',' .. orderPrice .. ',' .. amount)
                isCompleted = true
                break
            else
                -- 未交易完成
                amount = redis.call('DECIMAL', 'SUB', amount, orderAmount)
                table.insert(resultSet, orderId .. ',' .. orderPrice .. ',' .. orderAmount)
                orderId = redis.call('LPOP', orderQueueName)
            end
        end -- end of while

        -- 从 `price` 队列里 移除 `空的 order 队列`
        local orderQueueLength = redis.call('LLEN', orderQueueName)
        if orderQueueLength == 0 then
            redis.call('ZREM', priceQueueName, orderPrice)
        end
        -- 交易已完成
        if isCompleted then
            break
        end
    end -- end of for
    return resultSet
end

return match_orders(ARGV[1], ARGV[2], ARGV[3], ARGV[4])
