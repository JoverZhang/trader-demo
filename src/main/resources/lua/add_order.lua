--[[
返回值:
    0: 成功覆盖原挂单
    1: 成功添加挂单到 `order 队列`
    2: 成功添加挂单到 `order 队列`, 并将 `order 队列` 添加到 `price 队列`
]]
---@param namespace string
---@param id string
---@param price number
---@param amount number
---@return number
local function add_order(namespace, id, price, amount)
    local priceQueueName = namespace .. '::prices'
    local orderQueueName = namespace .. '::orders::' .. price
    local orderMapName = namespace .. '::map'

    -- 新增到 `map`, 如果是 覆盖了原来的挂单, 则直接返回
    if redis.call('HSET', orderMapName, id, amount) == 0 then
        return 0
    end
    -- 否则追加到 `当前 price` 的 `order 队列` 中
    redis.call('RPUSH', orderQueueName, id)

    -- 如果不是首次创建 `order 队列`, 则直接返回
    if redis.call('LLEN', orderQueueName) ~= 1 then
        return 1
    end
    -- 否则将 `order 队列` 添加到 `price 队列`
    redis.call('ZADD', priceQueueName, price, price)
    return 2
end

return add_order(ARGV[1], ARGV[2], ARGV[3], ARGV[4])
