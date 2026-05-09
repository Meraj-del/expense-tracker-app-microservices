local kong = kong
local http = require "resty.http"

local CustomAuthHandler = {
  PRIORITY = 1000,
  VERSION = "1.0",
}

function CustomAuthHandler:access(config)
  local auth_service_url = config.auth_service_url

  local httpc = http.new()
  local res, err = httpc:request_uri(auth_service_url, {
    method = "GET",
    headers = {
      ["Authorization"] = kong.request.get_header("Authorization")
    }
  })

  if not res then
    kong.log.err("Failed to call auth service: ", err)
    return kong.response.exit(500, { message = "Internal Server Error" })
  end

  if res.status ~= 200 then
    return kong.response.exit(res.status, { message = "Unauthorized" })
  end

  local user_id = res.body
  kong.service.request.set_header("X-User-ID", user_id)
end

return CustomAuthHandler
