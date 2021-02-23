---@type string
local namespace = ARGV[1]

local Order = { id = nil, price = nil, amount = nil }
function Order:new(id, price, amount)
    local o = {}
    o.id = id
    o.price = price
    o.amount = amount
    return o
end
function Order:toString(o)
    return o.id .. ',' .. o.price .. ',' .. o.amount
end

local OrderPool = { namespace = namespace }
local function pool_name ()
    return OrderPool.namespace .. '::prices'
end
---@param price string
local function queue_name (price)
    return OrderPool.namespace .. '::orders::' .. price
end
local function map_name ()
    return OrderPool.namespace .. '::map'
end
---@param srcPrice number
---@param tarPrice number
---@param isAscending boolean
local function validate_price_for_pop(srcPrice, tarPrice, isAscending)
    if isAscending then
        return tonumber(srcPrice) >= tonumber(tarPrice)
    else
        return tonumber(srcPrice) <= tonumber(tarPrice)
    end
end

---@param isAscending boolean
---@return Order
local function peek_first(isAscending)
    local prices
    if isAscending then
        prices = redis.call('ZRANGEBYSCORE', pool_name(), '-INF', '+INF', 'LIMIT', 0, 1)
    else
        prices = redis.call('ZREVRANGEBYSCORE', pool_name(), '+INF', '-INF', 'LIMIT', 0, 1)
    end
    if (prices[1] == nil) then
        return nil
    end
    local queueName = queue_name(prices[1])
    local orderIds = redis.call('LRANGE', queueName, 0, 0)
    local amount = redis.call('HGET', map_name(), orderIds[1])
    return Order:new(orderIds[1], prices[1], amount)
end

---@param order Order
local function update(order)
    redis.call('HSET', map_name(), order.id, order.amount)
end

---@param order Order
local function remove(order)
    -- 从 `map` 中删除 `挂单`, 如果 `挂单` 已经不存在, 则直接返回
    local deleted = redis.call('HDEL', map_name(), order.id)
    if deleted ~= 1 then
        return 0
    end

    -- 从 `队列` 中移除 `挂单`
    local queueName = queue_name(order.price)
    redis.call('LREM', queueName, 1, order.id)

    -- 当 `队列` 在移除挂单后仍存在 `挂单`, 则直接返回
    local queueLen = redis.call('LLEN', queueName)
    if queueLen ~= 0 then
        return 1
    end

    -- 从 `挂单池` 中移除空的 `队列`
    redis.call('ZREM', pool_name(), order.price)
    return 2
end

---@param price string
---@param amount string
---@param isAscending boolean
---@return table : [
--- remainingPrice, 剩余价格
--- remainingAmount, 剩余数量
--- orders 成功弹出的挂单s
---]
local function do_pop(price, amount, isAscending)
    local matchedOrders = {}
    local remainingAmount = amount

    local firstOrder = peek_first(isAscending)
    while firstOrder ~= nil and
            validate_price_for_pop(price, firstOrder.price, isAscending) and
            tonumber(remainingAmount) > 0 do
        -- 当 剩余数量 < 挂单的数量 时
        -- 挂单的数量 -= 剩余数量, 剩余数量 = 0
        if tonumber(remainingAmount) < tonumber(firstOrder.amount) then
            table.insert(matchedOrders, Order:toString(Order:new(
                    firstOrder.id, firstOrder.price, remainingAmount)))
            update(Order:new(
                    firstOrder.id, firstOrder.price,
                    redis.call('DECIMAL', 'SUB', firstOrder.amount, remainingAmount)))
            remainingAmount = 0
            -- 当 剩余数量 >= 挂单的数量 时
            -- 删除第一个挂单, 剩余数量 -= 挂单的数量
        else
            table.insert(matchedOrders, Order:toString(firstOrder))
            remove(firstOrder)
            remainingAmount = redis.call('DECIMAL', 'SUB', remainingAmount, firstOrder.amount)
        end

        firstOrder = peek_first(isAscending)
    end

    return { price, remainingAmount, matchedOrders }
end

---@param amount string
---@see do_pop(price, amount, isAscending)
local function pop_by_amount(amount)
    return do_pop(0, amount, false)
end

--return do_pop(ARGV[2], ARGV[3], ARGV[4] == 'true')
return pop_by_amount(ARGV[2])
