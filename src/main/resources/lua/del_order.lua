--[[
返回值:
    0: 挂单已不存在
    1: 成功将挂单从 `order 队列` 中移除
    2: 成功将挂单从 `order 队列` 中移除. 且 `order 队列` 已为空, 已从 `price 队列` 中被移除
]]
---@param namespace string
---@param id string
---@param price number 无作用, 仅为优化查找
---@return number
local function del_order(namespace, id, price)
    local priceQueueName = namespace .. '::prices'
    local orderQueueName = namespace .. '::orders::' .. price
    local orderMapName = namespace .. '::map'

    -- 从 `map` 中删除 `挂单`, 如果 `挂单` 已经不存在, 则直接返回
    local deleted = redis.call('HDEL', orderMapName, id)
    if deleted ~= 1 then
        return 0
    end
    -- 否则将 `挂单` 从 `order 队列` 中移除
    redis.call('LREM', orderQueueName, 1, id)

    -- 当 `order 队列` 在移除挂单后仍不为空, 则直接返回
    local orderQueueLength = redis.call('LLEN', orderQueueName)
    if orderQueueLength ~= 0 then
        return 1
    end
    -- 否则从 `price 队列` 中移除空的 `order 队列`
    redis.call('ZREM', priceQueueName, price)
    return 2
end

return del_order(ARGV[1], ARGV[2], ARGV[3])
