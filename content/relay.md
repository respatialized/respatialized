# // RELAY

<div class="f4">
A relay isn't the source of a signal or information. It just helps the signal travel further and more accurately.
</div>

Usage modes:

- *SMS* - text a designated number from a designated number with a message, the relay puts the message in a database and saves the metadata. Programs using the database can use the relay to send messages back. 
    - <span class="f8"> _oblique strategy_: send a message with an oracular glyph in it to the relay, and at random the oracle program will send one of those glyphed messages back.</span>
- *API* - some other data source is periodically emitting JSON data that needs to be parsed and filed. The relay endpoint enriches the data with its context and passes it off to the processing stack.
- *sync* - the relay hosts its own copy of a conflict-free replicated data type, which other remotes read to and write from via their own local copies.
- *post* - provide a layer of indirection between the source of posts and the platform they live on so the logic of the platform exerts less control over the poster.