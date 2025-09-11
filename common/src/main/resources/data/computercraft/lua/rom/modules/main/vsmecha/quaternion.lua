-- The MIT License
--
-- Copyright (c) 2015-2024 Richard Greenlees
-- Original located at org.joml.Quaterniond
-- Modified by Kevin Z <zyxkad@gmail.com>

local expect = require('rom/modules/main/cc/expect').expect

local Quaternion = {
	__name = "quaternion",
}
Quaternion.__index = Quaternion

local function safeAcos(angle)
	return math.acos(math.min(math.max(angle, -1), 1))
end

local function isFinite(v)
	return v == v and v ~= math.huge and v ~= -math.huge
end

local function isInstance(q)
	return type(q) == 'table' and getmetatable(q) == Quaternion
end

local function new(x, y, z, w)
	if isInstance(x) then
		x, y, z, w = x.x, x.y, x.z, x.w
	end
	expect(1, x, 'number', 'nil')
	expect(2, y, 'number', 'nil')
	expect(3, z, 'number', 'nil')
	expect(4, w, 'number', 'nil')
	return setmetatable({
		x = tonumber(x) or 0,
		y = tonumber(y) or 0,
		z = tonumber(z) or 0,
		w = tonumber(w) or 1,
	}, Quaternion)
end

function Quaternion:get()
	return self.x, self.y, self.z, self.w
end

function Quaternion:set(x, y, z, w)
	if isInstance(x) then
		x, y, z, w = x.x, x.y, x.z, x.w
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	self.x = x
	self.y = y
	self.z = z
	self.w = w
	return self
end

function Quaternion:add(x, y, z, w)
	if isInstance(x) then
		x, y, z, w = x.x, x.y, x.z, x.w
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	return self:addTo(x, y, z, w, self)
end

function Quaternion:addTo(x, y, z, w, dest)
	if isInstance(x) then
		x, y, z, w, dest = x.x, x.y, x.z, x.w, y
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	expect(5, dest, 'table')
	dest.x = self.x + x
	dest.y = self.y + y
	dest.z = self.z + z
	dest.w = self.w + w
	return dest
end

function Quaternion:sub(x, y, z, w)
	if isInstance(x) then
		x, y, z, w = x.x, x.y, x.z, x.w
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	return self:subTo(x, y, z, w, self)
end

function Quaternion:subTo(x, y, z, w, dest)
	if isInstance(x) then
		x, y, z, w, dest = x.x, x.y, x.z, x.w, y
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	expect(5, dest, 'table')
	dest.x = self.x - x
	dest.y = self.y - y
	dest.z = self.z - z
	dest.w = self.w - w
	return dest
end

function Quaternion:dot(x, y, z, w)
	if isInstance(x) then
		x, y, z, w = x.x, x.y, x.z, x.w
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	return self.x * x + self.y * y + self.z * z + self.w * w
end

function Quaternion:mul(x, y, z, w)
	if isInstance(x) then
		x, y, z, w = x.x, x.y, x.z, x.w
	end
	expect(1, x, 'number')
	if y == nil then
		return self:mul1To(x, self)
	end
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	return self:mulTo(x, y, z, w, self)
end

function Quaternion:mulTo(x, y, z, w, dest)
	if isInstance(x) then
		x, y, z, w, dest = x.x, x.y, x.z, x.w, y
	end
	expect(1, x, 'number')
	if isInstance(y) then
		return self:mul1To(x, y)
	end
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, w, 'number')
	expect(5, dest, 'table')
	dest.x = self.w * x + self.x * w + self.y * z - self.z * y
	dest.y = self.w * y - self.x * z + self.y * w + self.z * x
	dest.z = self.w * z + self.x * y - self.y * x + self.z * w
	dest.w = self.w * w - self.x * x - self.y * y - self.z * z
	return dest
end

function Quaternion:mul1(v)
	return self:mul1To(v, self)
end

function Quaternion:mul1To(v, dest)
	expect(1, v, 'number')
	expect(2, dest, 'table')
	dest.x = self.x * v
	dest.y = self.y * v
	dest.z = self.z * v
	dest.w = self.w * v
	return dest
end

function Quaternion:angle()
	return safeAcos(self.w)
end

function Quaternion:invert()
	return self:invertTo(self)
end

function Quaternion:invertTo(dest)
	expect(1, dest, 'table')
	local invNorm = 1 / self:lengthSquared()
	dest.x = -self.x * invNorm
	dest.y = -self.y * invNorm
	dest.z = -self.z * invNorm
	dest.w = self.w * invNorm
	return dest
end

function Quaternion:conjugate()
	return self:conjugateTo(self)
end

function Quaternion:conjugateTo(dest)
	expect(1, dest, 'table')
	dest.x = -self.x
	dest.y = -self.y
	dest.z = -self.z
	return dest
end

function Quaternion:lengthSquared()
	local x, y, z, w = self.x, self.y, self.z, self.w
	return x * x + y * y + z * z + w * w
end

function Quaternion:rotationYXZ(angleY, angleX, angleZ)
	expect(1, angleY, 'number')
	expect(2, angleX, 'number')
	expect(3, angleZ, 'number')

	local sx = math.sin(angleX * 0.5)
	local cx = math.cos(angleX * 0.5)
	local sy = math.sin(angleY * 0.5)
	local cy = math.cos(angleY * 0.5)
	local sz = math.sin(angleZ * 0.5)
	local cz = math.cos(angleZ * 0.5)

	local x = cy * sx
	local y = sy * cx
	local z = sy * sx
	local w = cy * cx
	self.x = x * cz + y * sz
	self.y = y * cz - x * sz
	self.z = w * sz - z * cz
	self.w = w * cz + z * sz
	return self
end

function Quaternion:slerp(target, alpha)
	return self:slerpTo(target, alpha, self)
end

function Quaternion:slerpTo(target, alpha, dest)
	expect(1, target, 'table')
	expect(2, alpha, 'number')
	expect(3, dest, 'table')
	local x, y, z, w = target.x, target.y, target.z, target.w
	local cosom = self.x * x + self.y * y + self.z * z + self.w * w
	local absCosom = math.abs(cosom)
	local scale0, scale1
	if 1 - absCosom > 1e-6 then
		local sinSqr = 1 - absCosom * absCosom
		local sinom = 1 / math.sqrt(sinSqr)
		local omega = math.atan2(sinSqr * sinom, absCosom)
		scale0 = math.sin((1 - alpha) * omega) * sinom
		scale1 = math.sin(alpha * omega) * sinom
	else
		scale0 = 1 - alpha
		scale1 = alpha
	end
	scale1 = cosom >= 0 and scale1 or -scale1
	dest.x = scale0 * self.x + scale1 * x
	dest.y = scale0 * self.y + scale1 * y
	dest.z = scale0 * self.z + scale1 * z
	dest.w = scale0 * self.w + scale1 * w
	return dest
end

function Quaternion:transform(x, y, z)
	if type(x) == 'table' then
		return self:transformTo(x.x, x.y, x.z, x)
	end
	return self:transformTo(x, y, z, vector.new())
end

function Quaternion:transformTo(x, y, z, dest)
	if type(x) == 'table' then
		x, y, z, dest = x.x, x.y, x.z, y
	end
	expect(1, x, 'number')
	expect(2, y, 'number')
	expect(3, z, 'number')
	expect(4, dest, 'table')

	local xx, yy, zz, ww = self.x * self.x, self.y * self.y, self.z * self.z, self.w * self.w
	local xy, xz, yz, xw = self.x * self.y, self.x * self.z, self.y * self.z, self.x * self.w
	local zw, yw, k = self.z * self.w, self.y * self.w, 1 / (xx + yy + zz + ww)
	dest.x = (xx - yy - zz + ww) * k * x + (2 * (xy - zw) * k * y + (2 * (xz + yw) * k) * z)
	dest.y = 2 * (xy + zw) * k * x + ((yy - xx - zz + ww) * k * y + (2 * (yz - xw) * k) * z)
	dest.z = 2 * (xz - yw) * k * x + (2 * (yz + xw) * k * y + ((zz - xx - yy + ww) * k) * z)
	return dest
end

function Quaternion:isFinite()
	return isFinite(self.x) and isFinite(self.y) and isFinite(self.z) and isFinite(self.w)
end

function Quaternion:__add(other)
	return self:addTo(other, new())
end

function Quaternion:__sub(other)
	return self:subTo(other, new())
end

function Quaternion:__mul(other)
	return self:mulTo(other, new())
end

function Quaternion:__div(other)
	return self:divTo(other, new())
end

function Quaternion:__unm()
	return self:mul1To(-1, new())
end

function Quaternion:__tostring()
	return self.x .. ',' .. self.y .. ',' .. self.z .. ',' .. self.w
end

function Quaternion:__eq(other)
	return self.x == other.x and self.y == other.y and self.z == other.z and self.w == other.w
end

return {
	isInstance = isInstance,
	new = new,
}
