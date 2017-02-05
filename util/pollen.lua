-- This is custom writer for pandoc based on the built-in sample
-- writer. It produces Pollen-equivalent output that is very similar
-- to that of pandoc's HTML writer.

-- There is one new feature: code blocks marked with class 'dot'
-- are piped through graphviz and images are included in the HTML
-- output using 'data:' URLs.
--
-- Invoke with: pandoc -t sample.lua
--
-- Note:  you need not have lua installed on your system to use this
-- custom writer.  However, if you do have lua installed, you can
-- use it to test changes to the script.  'lua sample.lua' will
-- produce informative error messages if your code contains
-- syntax errors.

-- Character escaping
local function escape(s, in_attribute)
  return s:gsub("[<>&\"']",
    function(x)
      if x == '<' then
        return '&lt;'
      elseif x == '>' then
        return '&gt;'
      elseif x == '&' then
        return '&amp;'
      elseif x == '"' then
        return '&quot;'
      elseif x == "'" then
        return '&#39;'
      else
        return x
      end
    end)
end

function trim (s)
  return (string.gsub(s, "^%s*(.-)%s*$", "%1"))
end

-- Helper function to convert an attributes table into
-- a string that can be put into Pollen tags:
-- e.g.: ◊span['class:"author" 'id:"primary" 'living:"true"]{Prof. Leonard}
--
local function attributes(attr)
  local attr_table = {}
  for x,y in pairs(attr) do
    if y and y ~= "" then
      table.insert(attr_table, ' #:' .. x .. ' "' .. escape(y,true) .. '"')
    end
  end
  return trim(table.concat(attr_table))
end

-- Run cmd on a temporary file containing inp and return result.
local function pipe(cmd, inp)
  local tmp = os.tmpname()
  local tmph = io.open(tmp, "w")
  tmph:write(inp)
  tmph:close()
  local outh = io.popen(cmd .. " " .. tmp,"r")
  local result = outh:read("*all")
  outh:close()
  os.remove(tmp)
  return result
end

-- Table to store footnotes, so they can be included at the end.
-- [Left in from sample writer; not needed in Pollen]
local notes = {}

-- Blocksep is used to separate block elements.
function Blocksep()
  return "\n\n"
end

-- This function is called once for the whole document. Parameters:
-- body is a string, metadata is a table, variables is a table.
-- This gives you a fragment.  You could use the metadata table to
-- fill variables in a custom lua template.  Or, pass `--template=...`
-- to pandoc, and pandoc will add do the template processing as
-- usual.
function Doc(body, metadata, variables)
  local buffer = {}
  local function add(s)
    table.insert(buffer, s)
  end
  -- add('#lang pollen\n')
  add(body)

--[[ NOT NEEDED, left in for example purposes:
  if #notes > 0 then
    add('<ol class="footnotes">')
    for _,note in pairs(notes) do
      add(note)
    end
    add('</ol>')
  end
--]]
  return table.concat(buffer,'\n')
end

-- The functions that follow render corresponding pandoc elements.
-- s is always a string, attr is always a table of attributes, and
-- items is always an array of strings (the items in a list).
-- Comments indicate the types of other variables.

function Str(s)
  return escape(s)
end

function Space()
  return " "
end

function SoftBreak()
  return "\n"
end

function LineBreak()
  return '\n'
end

function Emph(s)
  return "◊emph{" .. s .. "}"
end

function Strong(s)
  return "◊strong{" .. s .. "}"
end

function Subscript(s)
  return "◊sub{" .. s .. "}"
end

function Superscript(s)
  return "◊sup{" .. s .. "}"
end

function SmallCaps(s)
  return '◊smallcaps{' .. s .. '}'
end

function Strikeout(s)
  return '◊del{' .. s .. '}'
end

-- This assumes you have defined a ◊hyperlink tag function;
-- customize for your needs.
function Link(s, src, tit)
  return '◊link\["' .. src .. '"\]{' .. s .. "}"
end

-- This assumes you have defined a ◊figure tag function;
-- customize for your needs.
function Image(s, src, tit)
  return '◊figure\["' .. src .. "\"\]{" .. tit .. "}"
-- Alternate:
--  return "◊img\[#:src \"" .. src .. "\" #:title \"" ..
--         escape(tit,true) .. "\"\]{}"
end

function CaptionedImage(src, tit, caption)
   return "◊figure\[\"" .. src .. "\"\]{" ..
      caption .. "}"
end

function Code(s, attr)
  return "◊code{" .. s .. "}"
  -- OLD: return "<code" .. attributes(attr) .. ">" .. escape(s) .. "</code>"
end

function InlineMath(s)
  return "\\(" .. s .. "\\)"
end

function DisplayMath(s)
  return "\\[" .. s .. "\\]"
end

function Note(s)
--[[ Left in for example purposes:
  local num = #notes + 1
  -- insert the back reference right before the final closing tag.
  s = string.gsub(s,
          '(.*)</', '%1 <a href="#fnref' .. num ..  '">&#8617;</a></')
  -- add a list item with the note to the note table.
  table.insert(notes, '<li id="fn' .. num .. '">' .. s .. '</li>')
  -- return the footnote reference, linked to the note.
  return '<a id="fnref' .. num .. '" href="#fn' .. num ..
            '"><sup>' .. num .. '</sup></a>'
--]]
  return '◊numbered-note{' .. s .. '}'
end

function Span(s, attr)
  return "◊span\[" .. attributes(attr) .. "\]{" .. s .. "}"
end

function Cite(s, cs)
  local ids = {}
  for _,cit in ipairs(cs) do
    table.insert(ids, cit.citationId)
  end
  return "<span class=\"cite\" data-citation-ids=\"" .. table.concat(ids, ",") ..
    "\">" .. s .. "</span>"
end

function Plain(s)
  return s
end

function Para(s)
  -- return "◊p{" .. s .. "}"
  return s
end

-- lev is an integer, the header level.
function Header(lev, s, attr)
  if lev == 2 then
    return "◊section\[" .. attributes(attr) ..  "\]{" .. s .. "}"
  elseif  lev == 3 then
    return "◊subsection\[" .. attributes(attr) ..  "\]{" .. s .. "}"
  end
  return "◊h" .. lev .."\[" .. attributes(attr) ..  "\]{" .. s .. "}"
end

function BlockQuote(s)
  return "◊blockquote{\n" .. s .. "\n}"
end

function HorizontalRule()
  return "◊hr{}"
end

function CodeBlock(s, attr)
  -- If code block has class 'dot', pipe the contents through dot
  -- and base64, and include the base64-encoded png as a data: URL.
  if attr.class and string.match(' ' .. attr.class .. ' ',' dot ') then
    local png = pipe("base64", pipe("dot -Tpng", s))
    return '<img src="data:image/png;base64,' .. png .. '"/>'
  -- otherwise treat as code (one could pipe through a highlighter)
  else
    return "◊blockcode{" .. s .. "}"
  end
end

function BulletList(items)
  local buffer = {}
  for _, item in pairs(items) do
    table.insert(buffer, "◊item{" .. item .. "}")
  end
  return "◊ul{\n" .. table.concat(buffer, "\n") .. "\n}"
end

function OrderedList(items)
  local buffer = {}
  for _, item in pairs(items) do
    table.insert(buffer, "◊item{" .. item .. "}")
  end
  return "◊ol{\n" .. table.concat(buffer, "\n") .. "\n}"
end

-- Revisit association list STackValue instance.
function DefinitionList(items)
  local buffer = {}
  for _,item in pairs(items) do
    for k, v in pairs(item) do
      table.insert(buffer,"◊dt{" .. k .. "}\n    ◊dd{" ..
                        table.concat(v,"}\n    ◊dd{") .. "}")
    end
  end
  return "◊dl{\n" .. table.concat(buffer, "\n") .. "\n}"
end

-- Convert pandoc alignment to something HTML can use.
-- align is AlignLeft, AlignRight, AlignCenter, or AlignDefault.
function html_align(align)
  if align == 'AlignLeft' then
    return 'left'
  elseif align == 'AlignRight' then
    return 'right'
  elseif align == 'AlignCenter' then
    return 'center'
  else
    return 'left'
  end
end

-- Caption is a string, aligns is an array of strings,
-- widths is an array of floats, headers is an array of
-- strings, rows is an array of arrays of strings.
function Table(caption, aligns, widths, headers, rows)
  local buffer = {}
  local function add(s)
    table.insert(buffer, s)
  end
  add("◊table{")
  if caption ~= "" then
    add("◊caption{" .. caption .. "}")
  end
  if widths and widths[1] ~= 0 then
    for _, w in pairs(widths) do
      add('◊col\[\'width:"' .. string.format("%d%%", w * 100) .. '"\]{}')
    end
  end
  local header_row = {}
  local empty_header = true
  for i, h in pairs(headers) do
    local align = html_align(aligns[i])
    table.insert(header_row,'◊th\[\'align:"' .. align .. '"\]{' .. h .. '}')
    empty_header = empty_header and h == ""
  end
  if empty_header then
    head = ""
  else
    add('◊tr\[\'class:"header"\]{')
    for _,h in pairs(header_row) do
      add(h)
    end
    add('}')
  end
  local class = "even"
  for _, row in pairs(rows) do
    class = (class == "even" and "odd") or "even"
    add('◊tr\[\'class:"' .. class .. '"\]{')
    for i,c in pairs(row) do
      add('◊td\[\'align:"' .. html_align(aligns[i]) .. '"\]{' .. c .. '}')
    end
    add('}')
  end
  add('}')
  return table.concat(buffer,'\n')
end

function Div(s, attr)
  return "◊div\[" .. attributes(attr) .. "\]{\n" .. s .. "}"
end

-- The following code will produce runtime warnings when you haven't defined
-- all of the functions you need for the custom writer, so it's useful
-- to include when you're working on a writer.
local meta = {}
meta.__index =
  function(_, key)
    io.stderr:write(string.format("WARNING: Undefined function '%s'\n",key))
    return function() return "" end
  end
setmetatable(_G, meta)
