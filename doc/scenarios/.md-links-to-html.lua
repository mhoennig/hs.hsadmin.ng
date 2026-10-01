-- the Markdown reports link each other by their .md files, which only exist as .html next to the HTML reports
function Link(link)
  if not link.target:match("^%a[%w+.-]*:") then
    link.target = link.target:gsub("%.md$", ".html"):gsub("%.md#", ".html#")
  end
  return link
end
