---@param namespace string
local function fetch_all_orders(namespace)
    local priceQueueName = namespace .. '::prices'
    local orderQueueNamePrefix = namespace .. '::orders::'
    local orderMapName = namespace .. '::map'
    local resultSet = {}

    -- 遍历现有的所有 `price`
    local priceQueue = redis.call('ZRANGE', priceQueueName, 0, -1)
    for _, price in ipairs(priceQueue) do
        local result = {}

        -- 遍历当前 `price` 的所有订单
        local orderIds = redis.call('LRANGE', orderQueueNamePrefix .. price, 0, -1)
        for _, orderId in ipairs(orderIds) do
            local amount = redis.call('HGET', orderMapName, orderId)
            table.insert(result, orderId .. ',' .. price .. ',' .. amount)
        end
        table.insert(resultSet, result)
    end
    return resultSet
end

return fetch_all_orders(ARGV[1])
